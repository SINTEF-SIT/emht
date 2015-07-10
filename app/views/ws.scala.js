@import play.i18n._

var WebSocketManager = (function ($, WS) {
    /* Private methods and fields */

    // open pewpew with websocket
    var socket = new WS('@routes.Application.wsInterface().webSocketURL(request)');

    // Event handling function
    var writeMessages = function (event) {
        var data = JSON.parse(event.data);
        var action = data.action;

        console.log("[WS] Event received: " + action);
        console.log("[WS] Event data: " + JSON.stringify(data, null, 4));

        if (null != action) {
            switch (action) {
                case "alarmNew":
                    Alarms.addAlarm(data.alarm);
                    break;

                case "alarmClosed":
                    Alarms.removeAlarm(Alarms.getAlarm(data.alarm.id));
                    break;

                case "alarmAssigned":
                    var alarm = Alarms.getAlarm(data.alarm.id);
                    if (data.alarm.attendant.id !== Alarms.me().id) {
                        $('#callee_info_modal').modal('hide');
                        alarm.DOM.hide();
                        alarm.DOM = [];
                        alarm.data = data.alarm;
                        alarm.state = 'assigned';
                        Alarms.gui.resetAlarmCount();
                    }
                    break;

                case "alarmDispatched":
                    var alarm = Alarms.getAlarm(data.alarm.id);
                    if (data.alarm.attendant.id !== Alarms.me().id) {
                        alarm.DOM = [];
                        alarm.data = data.alarm;
                        alarm.state = 'followup';
                        Alarms.gui.resetAlarmCount();
                    }
                    break;

                case "alarmExternalFollowupNotify":
                    var id = data.alarm.id;
                    var elementId ="#Alarm" + id;
                    // check if alarm does not have a recurring icon
                    if($(elementId).find('.recurring-icon').length == 0){
                        var recurringImage = '<img src="/assets/images/recurring.png" class="img-thumbnail pull-left recurring-icon" width="48" height="48"/>';
                        var alarm = $(elementId).children(":first").after(recurringImage); // find list item and add the recurringImage after its first symbol
                        $(elementId).parent().prepend($(elementId));// move it to top of the list
                    }
                    break;

                case "alarmOpenExpired":

                    var clockImage = '<img src="/assets/images/clock.png" class="img-thumbnail pull-left clock-icon" width="48" height="48"/>';
                    var alarm = $("#Alarm" + data.alarm.id).children(":first").after(clockImage); // find list item and add the timer after its type symbol

                    break;

                case "alarmResolutionExpired":

                    var clockImage = '<img src="/assets/images/clock.png" class="img-thumbnail pull-left clock-icon" width="48" height="48"/>';
                    var alarm = $("#Alarm" + data.alarm.id).children(":first").after(clockImage); // find list item and add the timer after its type symbol

                    break;

                case "alarmFinished":
                    console.log("finishedAlarm received on alarm id: " + data.alarm.id);

                    var id = data.alarm.id;
                    var a = Alarms.getAlarm(id);
                    if (a === null) {
                        console.log("finishedAlarm alarm id was invalid for this client.");
                        return;
                    }
                    // We need to check this since we can have alarms not assigned to us that might not have
                    // a DOM object initialized yet.
                    if (a.DOM !== null && a.DOM !== undefined) {
                        if (a.DOM.children('.finished-icon').length > 0) return;
                        var finishedImage = '<img src="/assets/images/finished.png" class="img-thumbnail pull-left finished-icon" width="48" heigh="48"/>';
                        a.DOM.children('.clock-icon').remove();
                        a.DOM.children(':first').after(finishedImage);

                        a.DOM.children('.dispatchedTo').html('');
                    }

                    a.state = 'finished';

                    break;

                case "monitorStatistics":
                    var stats = data.stats;

                    $('#stats-total').text(stats.totalIncidents);
                    $('#stats-total-above-assignment-threshold').text(stats.totalIncidentsAboveAssignmentThreshold);
                    $('#stats-max-response-time').text(stats.maximumAssignmentTime / 1000);
                    $('#stats-average-response-time').text(stats.averageResponseTime / 1000);
            }
        }
    }

    /* Public methods inside return object */
    return {
        init: function () {
            socket.onmessage = writeMessages;
        }
    }
})(jQuery, window['MozWebSocket'] ? window['MozWebSocket'] : WebSocket)
