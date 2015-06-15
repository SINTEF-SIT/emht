@import play.i18n._

var WebSocketManager = (function ($, WS) {
    /* Private methods and fields */

    // open pewpew with websocket
    var socket = new WS('@routes.Application.wsInterface().webSocketURL(request)');

    // Event handling function
    var writeMessages = function (event) {
        var data = JSON.parse(event.data);
        var action = data.action.action;

        if (null != action) {
            switch (action) {
                case "addAlarm":
                    var alarm = data.alarm;
                    var time = new Date(alarm.openingDate);
                    var formatedTime = $.format.date(time, "dd/MM HH:mm")
                    var listItem = '<a href="#" idnum="'+ alarm.id +'" id="Alarm' + alarm.id  + '"  onclick="selectOpenAlarm(' + alarm.id + ',' + alarm.callee.id + ');return false;" class="list-group-item alarmItem">' +
                        '<img src="/assets/images/' + alarm.type + '.png" class="img-thumbnail pull-left type-icon" data-type="'+ alarm.type + '" width="48" height="48"/>' +
                        '<h4 class="list-group-item-heading"> @Messages.get("listitem.arrived") '+ formatedTime  +' </h4>' +
                        '<p class="list-group-item-text">@Messages.get("listitem.callee") ' + alarm.callee.name + ' ' + alarm.callee.phoneNumber + '</p>';
                    $("#unassignedAlarmList").append(listItem);
                    Alarms.gui.resetAlarmCount();
                    break;
                case "removeAlarm":
                    var id = data.alarmId;
                    var elementId ="#Alarm" + id;
                    $(elementId).remove();
                    Alarms.gui.resetAlarmCount();
                    break;
                case "notifyFollowup":
                    var id = data.alarmId;
                    var elementId ="#Alarm" + id;
                    // check if alarm does not have a recurring icon
                    if($(elementId).find('.recurring-icon').length == 0){
                        var recurringImage = '<img src="/assets/images/recurring.png" class="img-thumbnail pull-left recurring-icon" width="48" height="48"/>';
                        var alarm = $(elementId).children(":first").after(recurringImage); // find list item and add the recurringImage after its first symbol
                        $(elementId).parent().prepend($(elementId));// move it to top of the list
                    }
                    break;
                case "addTimeNotification":

                    var clockImage = '<img src="/assets/images/clock.png" class="img-thumbnail pull-left clock-icon" width="48" height="48"/>';
                    var alarm = $("#Alarm" + data.alarmId).children(":first").after(clockImage); // find list item and add the timer after its type symbol

                    break;
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
