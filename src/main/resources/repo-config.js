(function ($) {
  require(['bitbucket/util/state'], function (state) {
    var pluginConfigUrl = AJS.contextPath() + "/rest/notify-jenkins/1.0/config";
    var repoConfigUrl;

    var pluginConfigData = {
      instances: []
    };

    var repoConfigData = {};

    $(document).ready(function () {
      repoConfigUrl = AJS.contextPath() + "/rest/notify-jenkins/1.0/" + state.getProject().key + "/" + state.getRepository().slug + "/config";

      loadData();

      $("#pluginConfigForm").on('submit', onSubmitForm);
    });

    function onSubmitForm(e) {
      e.preventDefault();
      saveData();
    }

    function loadData() {
      $.when(
        $.get(pluginConfigUrl),
        $.get(repoConfigUrl)
      ).done(function(pluginConfig, repoConfig) {
        console.log('found repoConfigData', repoConfig[0]);
        console.log('found plugin data', pluginConfig[0])
        repoConfigData = repoConfig[0];
        pluginConfigData = pluginConfig[0];
        render();
      });
    }

    function getCloneUrl() {
      return pluginConfigData.repoUrlPattern
        .replace('{{PROJECT_KEY}}', state.getProject().key)
        .replace('{{REPO_SLUG}}', state.getRepository().slug);
    }

    function saveData() {
      repoConfigData.active = $('#active').is(':checked');
      repoConfigData.jenkinsInstance = $('#jenkinsInstance').val();
      repoConfigData.triggerRepoPush = $('#triggerRepoPush').is(':checked');
      repoConfigData.triggerBranchCreated = $('#triggerBranchCreated').is(':checked');
      repoConfigData.triggerBranchDeleted = $('#triggerBranchDeleted').is(':checked');
      repoConfigData.triggerFileEdit = $('#triggerFileEdit').is(':checked');
      repoConfigData.triggerPRMerged = $('#triggerPRMerged').is(':checked');

      $.ajax({
        url: repoConfigUrl,
        type: "PUT",
        dataType: 'json',
        contentType: "application/json",
        data: JSON.stringify(repoConfigData),
        processData: false,
        error: function (res) {
          console.log('error response is ', res)
          AJS.flag({
            close: 'auto',
            type: 'error',
            title: 'Error Updating Settings',
            body: res.responseJSON.join('<br/>')

          });
        },
        success: function () {
          AJS.flag({
            close: 'auto',
            type: 'success',
            title: 'Updated Settings'
          });
          render();
        }
      });
    }

    function render() {
      renderJenkinsInstances();

      $('#active').prop('checked', repoConfigData.active);
      $('#cloneUrl').text(getCloneUrl());
      $('#triggerRepoPush').prop('checked', repoConfigData.triggerRepoPush);
      $('#triggerBranchCreated').prop('checked', repoConfigData.triggerBranchCreated);
      $('#triggerBranchDeleted').prop('checked', repoConfigData.triggerBranchDeleted);
      $('#triggerFileEdit').prop('checked', repoConfigData.triggerFileEdit);
      $('#triggerPRMerged').prop('checked', repoConfigData.triggerPRMerged);

      $('#pluginConfigForm').show();
    }

    function renderJenkinsInstances() {
      var $select = $('#jenkinsInstance');
      $select.find('option').remove()

      pluginConfigData.instances.forEach(function(instance) {
        $select.append(
          $('<option value="' + instance.code + '">' + instance.name + '</option>')
        )
      });

      $select.val(repoConfigData.jenkinsInstance);
    }

  });
})(AJS.$ || jQuery);


