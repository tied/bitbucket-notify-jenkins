(function ($) {
  var url = AJS.contextPath() + "/rest/notify-jenkins/1.0/config";
  var data = {
    instances: []
  };

  $(document).ready(function () {
    loadData();

    $("#admin").on('submit', onSubmitForm);
    $("#dialog-trigger").on('click', onDialogOpen);
    $("#dialog-close-button").on('click', onDialogClose);
    $("#dialog-ok-button").on('click', onDialogOk);
    $('#jenkinsInstances').on('click', ".delete-jenkins-instance", onInstanceDelete);
  });

  function onSubmitForm(e) {
    e.preventDefault();
    saveData();
  }

  function onDialogOpen(e) {
    e.preventDefault();
    clearDialogForm();
    AJS.dialog2("#jenkins-dialog").show();
  }

  function onDialogClose(e) {
    e.preventDefault();
    AJS.dialog2("#jenkins-dialog").hide();
  }

  function onInstanceDelete(e) {
    e.preventDefault();
    var code = $(this).closest('tr').find('td.jenkins-code').text();
    console.log('try to remove ' + code)
    removeInstance(code);
  }

  function onDialogOk() {
    var jenkinsCode = $('#addJenkinsCode').val();
    var jenkinsName = $('#addJenkinsName').val();
    var jenkinsUrl = $('#addJenkinsUrl').val();

    // only add if all data entered
    if ($.trim(jenkinsCode) !== '' && $.trim(jenkinsName) !== '' && $.trim(jenkinsUrl) !== '') {
      addInstance(jenkinsCode, jenkinsName, jenkinsUrl);
      AJS.dialog2("#jenkins-dialog").hide();
    }
  }

  function clearDialogForm() {
    $('#addJenkinsCode').val('');
    $('#addJenkinsName').val('');
    $('#addJenkinsUrl').val('');
  }

  function loadData() {
    $.ajax({
      url: url,
      dataType: "json"
    }).success(function (config) {
      data = config;
      render();
    }).error(function () {
      var myFlag = AJS.flag({
        type: 'error',
        title: 'Error Fetching Settings'
      });
    });
  }

  function saveData() {
    //ensure data is up to date
    data.repoUrlPattern = $('#repoUrlPattern').val();

    $.ajax({
      url: url,
      type: "PUT",
      dataType: 'json',
      contentType: "application/json",
      data: JSON.stringify(data),
      processData: false,
      error: function () {
        var myFlag = AJS.flag({
          type: 'error',
          title: 'Error Updating Settings'
        });
      },
      success: function () {
        var myFlag = AJS.flag({
          type: 'success',
          title: 'Updated Settings'
        });
      }
    });
  }

  function addInstance(code, name, url) {
    data.instances.push({code: code, name: name, url: url});
    render();
  }

  function removeInstance(code) {
    data.instances = data.instances.filter(function (instance) {
      return instance.code !== code;
    });
    render();
  }

  function render() {
    renderAllInstances();
    $('#repoUrlPattern').val(data.repoUrlPattern);

    $('#admin').show();
  }

  function renderAllInstances() {
    $("#jenkinsInstances tbody").empty();

    data.instances.forEach(renderInstance);

    if (data.instances.length === 0) {
      $("#jenkinsInstances").hide();
    } else {
      $("#jenkinsInstances").show();
    }
  }

  function renderInstance(instance) {
    var newRowHtml = [
      '<tr>',
      '<td class="jenkins-code" />',
      '<td class="jenkins-name" />',
      '<td class="jenkins-url" />',
      '<td>',
      '<button class="aui-button aui-button-link delete-jenkins-instance">Delete</button>',
      '</td>',
      '</tr>'].join('\n');

    $('#jenkinsInstances > tbody:last-child').append(newRowHtml);
    $('#jenkinsInstances > tbody tr:last td.jenkins-code').text(instance.code)
    $('#jenkinsInstances > tbody tr:last td.jenkins-name').text(instance.name)
    $('#jenkinsInstances > tbody tr:last td.jenkins-url').text(instance.url)
  }
})(AJS.$ || jQuery);


