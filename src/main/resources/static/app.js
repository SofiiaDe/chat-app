let stompClient = null;

function setConnected(connected) {
    $("#connect").prop("disabled", connected);
    $("#disconnect").prop("disabled", !connected);

    if (connected) {
        $("#conversation").show();
    }
    else {
        $("#conversation").hide();
    }
    $("#chat").html("");
    document.getElementById('response').innerHTML = '';
}

function connect() {
    const socket = new SockJS('/chat');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        setConnected(true);
        console.log('Connected: ' + frame);
        stompClient.subscribe('/topic/messages', function (chat) {
            showChat(JSON.parse(chat.body).content);
        });
    });
}

function disconnect() {
    if (stompClient !== null) {
        stompClient.disconnect();
    }
    setConnected(false);
    console.log("Disconnected");
}

// function sendName() {
//     stompClient.send("/app/chat", {}, JSON.stringify({'name': $("#name").val()}));
// }

function sendMessage() {
    const from = document.getElementById('from').value;
    const text = document.getElementById('text').value;
    stompClient.send("/app/chat", {}, JSON.stringify({'from':from, 'text':text}));
}

function showChat(message) {
    const response = document.getElementById('response');
    const p = document.createElement('p');
    p.style.wordWrap = 'break-word';
    p.appendChild(document.createTextNode(message.from + ": "
        + message.text + " (" + message.time + ")"));
    response.appendChild(p);
    // $("#chat").append("<tr><td>" + message + "</td></tr>");
}

$(function () {
    $("form").on('submit', function (e) {
        e.preventDefault();
    });
    $( "#connect" ).click(function() { connect(); });
    $( "#disconnect" ).click(function() { disconnect(); });
    $( "#send" ).click(function() { sendMessage(); });
});

// Another functions to be used

// function setConnected(connected) {
//     document.getElementById('connect').disabled = connected;
//     document.getElementById('disconnect').disabled = !connected;
//     document.getElementById('conversationDiv').style.visibility
//         = connected ? 'visible' : 'hidden';
//     document.getElementById('response').innerHTML = '';
// }

// function connect() {
//     var socket = new SockJS('/chat');
//     stompClient = Stomp.over(socket);
//     stompClient.connect({}, function(frame) {
//         setConnected(true);
//         console.log('Connected: ' + frame);
//         stompClient.subscribe('/topic/messages', function(messageOutput) {
//             showMessageOutput(JSON.parse(messageOutput.body));
//         });
//     });
// }


// function sendMessage() {
//     var from = document.getElementById('from').value;
//     var text = document.getElementById('text').value;
//     stompClient.send("/app/chat", {},
//         JSON.stringify({'from':from, 'text':text}));
// }
