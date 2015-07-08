@import play.i18n._

var DEBUG = true;

var Actions = (function ($) {
    /* Private methods here */

    /* Public methods inside return object */
    return {
        init: function () {
            $(".dispatch-ring-btn").on('click', function(e) {
                e.preventDefault();
                var activeAlarm = Alarms.getActiveAlarm();
                if (activeAlarm === null) return alert('No active alarm');
                if (activeAlarm.protected) return alert('Alarm is protected. Cannot modify.');
                $('#calling_modal').modal("show")
            });
            $(".dispatch-send-btn").on('click', function(e) {
                e.preventDefault();
                var activeAlarm = Alarms.getActiveAlarm();
                if (activeAlarm === null) return alert('No active alarm');
                if (activeAlarm.protected) return alert('Alarm is protected. Cannot modify.');

                $("#dispatch_data_modal").find(':checkbox').each(
                    function() {
                        $(this).prop("checked", "checked");
                    }
                );

                // show modal
                $('#dispatch_data_modal').modal("show");
            });

            // setting up schedule time modal
            $(".schedule-btn").on('click', function(e) {
                e.preventDefault();
                var activeAlarm = Alarms.getActiveAlarm();
                if (activeAlarm === null) return alert('No active alarm');
                if (activeAlarm.protected) return alert('Alarm is protected. Cannot modify.');
                $("#schedule_time_modal").find(':checkbox').each(
                    function() {
                        $(this).prop("checked", "checked");
                    }
                );

                // show modal
                $('#schedule_time_modal').modal("show");
            });

            $("#schedule-time-picker").datetimepicker();
            $("#schedule_time_modal_btn").click(Actions.closeCaseAtClosing);
            // end of setting up schedule time modal

            $("#closeCaseActionsButton").click(Actions.closeCaseAtClosing);
            $("#saveAndFollowUpButton").click(Actions.saveAndFollowupAtClosing);

            // setting up organization tables so that the rows can be selected
            $("#kontaktpersonerTable > tbody").on("click","tr",Actions.organizationTableClick);
            $("#ambulerendeTable > tbody").on("click","tr",Actions.organizationTableClick);
            $("#legevaktTable > tbody").on("click","tr",Actions.organizationTableClick);
            $("#tpTable > tbody").on("click","tr",Actions.organizationTableClick);
            $("#hjemmesykepleienTable > tbody").on("click","tr",Actions.organizationTableClick);

            Actions.reset();
            // make sure that no phone is expanded
        },

        organizationTableClick: function () {
            $(this).closest('table').find('tr.active').removeClass('active');
            $(this).addClass('active');
        },

        reset: function () {
            $("#dynamicDispatchButtons").find(".in").removeClass("in");
        },

        //simple function that just gets the data from the  page and package it
        //into a json object
        getUpdatedAlarmFromPage: function () {
            var patientId = $('#dynamicPatientInfo').find('#patientId').val();
            var alarmId = Alarms.getActiveAlarm().id;
            var notes = $('#globalNotesBox').val();

            var occuranceAddress = $('#incidentAddress').val();

            var updatedAlarm = Alarms.getActiveAlarm().data;
            updatedAlarm.notes = notes;
            updatedAlarm.assessment = Assessment.getAssessmentLog();
            updatedAlarm.occuranceAddress = occuranceAddress;

            return updatedAlarm;
        },

        closeCaseAtClosing: function () {
            var activeAlarm = Alarms.getActiveAlarm();
            if (activeAlarm === null) return alert('No active alarm');
            if (activeAlarm.protected) return alert('Alarm is protected, cannot modify.');
            var closeCase = function () {
                var updatedAlarm = Actions.getUpdatedAlarmFromPage();

                myJsRoutes.controllers.Application.closeCase().ajax({
                    data : JSON.stringify(updatedAlarm),
                    contentType : 'application/json',
                    success : function (data) {
                        Alarms.removeAlarm(Alarms.getActiveAlarm());
                    }
                });
            };
            console.log('Close Case called on alarm: ' + activeAlarm);
            // Add confirmation dialog box if there is an attempt to close a non-finished alarm assigned to caretaker
            if (activeAlarm.data.mobileCareTaker !== null && activeAlarm.isFollowup()) {
                if (confirm('@Messages.get("actions.button.close.closenonfinished")')) {
                    closeCase();
                }
            } else {
                closeCase();
            }
        },

        saveAndFollowupAtClosing: function (followUp) {
            var activeAlarm = Alarms.getActiveAlarm();
            if (activeAlarm === null) return alert('No active alarm');
            if (activeAlarm.data.patient === null) return alert('No patient selected!');
            if (activeAlarm.protected) return alert('Alarm is protected. Cannot modify.');

            var updatedAlarm = Actions.getUpdatedAlarmFromPage();

            // If we have a special followUp payload, add it to the alarm
            if (followUp !== null && followUp !== undefined) {
                console.log("saveAndFollowUpAtClosing received payload: ", followUp);
                if (followUp.type === 'mobileCareTaker') {
                    updatedAlarm.mobileCareTaker = followUp.id;
                    console.log("saveAndFollowUpAtClosing type was mobileCareTaker with ID: " + updatedAlarm.mobileCareTaker);
                }
            }

            myJsRoutes.controllers.Application.saveAndFollowupCase().ajax({
                data : JSON.stringify(updatedAlarm),
                contentType : 'application/json',
                success : function (data) {
                    Alarms.getActiveAlarm().data = data;
                    Alarms.gui.moveAlarmToFollowUpList();
                }// end of success
            });// end of ajax call
        },

        actionsDataSent: function () {
            $('#send_confirmation_modal').modal();
        }
    }
})(jQuery);
