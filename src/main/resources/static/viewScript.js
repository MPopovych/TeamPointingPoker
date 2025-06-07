// utility
const showToast = (msg, type = "info") => {
    const toast = document.createElement("div");
    toast.className = `px-4 py-2 rounded-xl shadow-md text-white ${type === "error" ? "bg-red-600" : "bg-green-600"}`;
    toast.textContent = msg;
    document.getElementById("toast-container").appendChild(toast);
    setTimeout(() => toast.remove(), 3000);
};

// ws
let stompClient = null;
const connect = () => {
    if (stompClient && stompClient.connected) return;

    const socket = new SockJS(`${window.location.origin.replace(/^http/, 'http')}/ws`);
    stompClient = Stomp.over(socket);

    stompClient.connect({}, frame => {
        console.log('Connected:', frame);
        showToast('Connected');

        const handleSync = evt => {
            const msg = JSON.parse(evt.body);
            if (msg.type === 'ERROR') showToast(msg.data, 'error');
            else if (msg.type === 'NOTIFICATION') showToast(msg.data);
            else if (msg.type === 'SYNC') updateSessionView(msg.data);
            else if (msg.type === 'SYNC_META') updateSessionMetaOnMetaSync(msg.data);
            else if (msg.type === 'USER_LIST_UPDATE') updateActiveUsers(msg.data.observers)
        };

        stompClient.subscribe(`/topic/session/${sessionData.id}/sync`, handleSync);
        stompClient.subscribe(`/user/topic/session/${sessionData.id}/sync`, handleSync);

        stompClient.send(`/app/${sessionData.id}/observe`, {});
    }, error => {
        console.error('STOMP Error:', error);
        showToast('Connection failed', 'error');

        reconnectInterval = setInterval(() => {
            console.log("Retrying STOMP connection...");
            connect();
        }, 5000); // Retry every 5 seconds
    });
};

// ws outgoing
const createNewRound = () => {
    if (!stompClient?.connected) return showToast('Not connected', 'error');
    stompClient.send(`/app/${sessionData.id}/newRound`, {});
};

const sendVote = (roundId, teamId, voteId) => {
    stompClient.send(`/app/${sessionData.id}/vote`, {}, JSON.stringify({
        type: 'VOTE',
        data: { roundId, teamId, voteId }
    }));
};

const toggleReveal = (roundId) => {
    stompClient.send(`/app/${sessionData.id}/toggleReveal`, {}, JSON.stringify({
        type: 'REVEAL',
        data: { roundId }
    }));
};

let metaDebounceTimer = null;
const debounceMetaUpdate = () => {
    if (metaDebounceTimer) clearTimeout(metaDebounceTimer);
    metaDebounceTimer = setTimeout(() => {
        if (!stompClient?.connected) return;
        stompClient.send(`/app/${sessionData.id}/editSession`, {}, JSON.stringify({
            type: 'EDIT_SESSION',
            data: {
                title: document.getElementById("session-title").value,
                description: document.getElementById("session-description").value
            }
        }));
    }, 500);
};

// reconnect
document.addEventListener("DOMContentLoaded", () => {
    ["session-title", "session-description"].forEach(id =>
        document.getElementById(id).addEventListener("input", debounceMetaUpdate)
    );
    connect();
});

// element updates
const updateSessionMeta = (title, description) => {
    document.getElementById('session-title').value = title || '';
    document.getElementById('session-description').value = description || '';
};

const updateSessionMetaOnMetaSync = data => updateSessionMeta(data.title, data.description);

const updateSessionView = data => {
    updateSessionMeta(data.title, data.description);
    updateActiveUsers(data.observers || {});
    updateRounds(data, data.members || {});
};

const updateActiveUsers = members => {
    const el = document.getElementById('active-users-list');
    el.innerHTML = Object.values(members).map(u =>
        `<span class="bg-gray-700 hover:bg-gray-600 text-gray-200 text-xs font-medium px-2 py-1 rounded-full transition-all">${u.name}</span>`
    ).join('');
};

const updateRounds = (data, members) => {
    const container = document.getElementById('rounds');
    const prevRoundCount = container.childElementCount;
    const rounds = data.rounds || {};
    container.innerHTML = '';
    const keys = Object.keys(rounds);

    const shouldNavigate = prevRoundCount != keys.length

    keys.forEach((roundId, index) => {
        const round = rounds[roundId];
        const isActive = index === keys.length - 1;
        const el = document.createElement('div');
        el.className = 'inline-block w-full max-w-[1200px] shrink-0 bg-gray-800 rounded-xl p-4 shadow';
        el.dataset.roundId = roundId;

        const teamBlocks = Object.entries(round.teamStateMap).map(([teamName, team]) => {
            const votes = sessionData.config.points.map(p =>
                `<button onclick="sendVote('${roundId}', '${teamName}', '${p.id}')"
                                class="w-full max-w-[80px] bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-semibold px-1 py-1 rounded-lg mb-1">${p.label}</button>`
            ).join('');

            const results = Object.entries(team.usersByScore).map(([uid, point]) => {
                const name = members[uid]?.name || uid;
                const value = team.open ? point.label : '• • •';
                return `<li class="flex justify-between text-sm text-gray-200"><span>${name}</span><span>${value}</span></li>`;
            }).join('');

            return `
                    <div class="bg-gray-900 rounded-xl p-4">
                        <h5 class="text-sm font-medium text-indigo-300 mb-3">Team: ${teamName}</h5>
                        <div class="flex gap-4">
                            <div class="flex flex-col w-28">${votes}</div>
                            <ul class="flex-1 space-y-1">${results}</ul>
                        </div>
                        <div class="text-xs text-right mt-3 ${team.open ? 'text-green-400' : 'text-yellow-400'}">
                            ${team.open ? (team.average != null ? `Avg: ${team.average}` : 'Votes revealed') : 'Voting in progress…'}
                        </div>
                    </div>`;
        }).join('');

        const roundStats = (round.open && (round.average != null || round.sum != null)) ? `
            <div class="mt-4 border-t border-gray-600 pt-3 text-sm text-gray-300 text-right">
                ${round.sum != null ? `<span class="mr-4">Sum: ${round.sum}</span>` : ''}
                ${round.average != null ? `<span>Avg: ${round.average}</span>` : ''}
            </div>
        ` : '';

        el.innerHTML = `
                    <div class="mb-2">
                        <h4 class="text-lg font-semibold text-white">${round.title || `Round ${index + 1}`}</h4>
                        <p class="text-sm text-gray-400">${round.comment || ''}</p>
                    </div>
                    <div class="mb-4 text-right">
                        <button onclick="toggleReveal('${roundId}')"
                                class="bg-purple-500 hover:bg-purple-600 text-white text-sm font-medium px-3 py-1 rounded-md">
                            ${round.forceOpen ? 'Close Votes' : 'Reveal Votes'}
                        </button>
                    </div>
                    <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">${teamBlocks}</div>
                    ${roundStats}
                `;
        container.appendChild(el);

        if (isActive && shouldNavigate) {
            setTimeout(() => el.scrollIntoView({ behavior: 'smooth', inline: 'center' }), 100);
        }
    });
};