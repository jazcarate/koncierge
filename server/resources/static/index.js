function runEval(event){
    event.preventDefault();

    var form = event.target;
    var context = form.context.value;
    var experiment = form.experiment.value

    if (history.pushState){
      var newurl = window.location.protocol + "//" + window.location.host + window.location.pathname + '?context=' + encodeURI(context) + '&experiment=' + encodeURI(experiment);
      window.history.pushState ( { path: newurl }, '', newurl);
    }

    result.innerHTML = 'Evaluating...';
    fetch(form.action, {
       method: form.method,
       headers: {
         'Accept': 'application/json',
         'Content-Type': 'application/json'
       },
       body: JSON.stringify({
         context: context,
         experiment: experiment
       })
     })
      .then(response => {
        if(response.ok)
            return response.json()
        return response.text()
            .then(err => Promise.reject(err));
      })
      .then(data => {
        var result = document.getElementById("result");
        result.innerHTML = '';

        if(data[0].length == 0){
            result.textContent = "No experiment matched.";
        } else {
            data.forEach(function(experiment){
                var li = document.createElement("li")
                li.textContent = experiment.join(".");
                result.appendChild(li)
            });
        }
      })
      .catch(err => {
        result.textContent = '[ERROR]: ' + err;
      });

    return false;
}

function pretty(elementId){
    var element = document.getElementById(elementId);
    try{
        var newTest = JSON.stringify(RJSON.parse(element.value), null, 4);
        element.value = newTest;
    } catch (error) {
       console.error(error);
     }
}