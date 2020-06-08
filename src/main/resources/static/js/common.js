function download() {
    var form = document.forms["resource_prop"];
    post(form, downloadCallback);
}
function post(form, responseCallback) {
    var data = new FormData(form);
    //服务器上传地址
    var url = "/download"  ;
    var xhr = new XMLHttpRequest();
    xhr.open("post", url, true);
    //允许跨域
    xhr.withCredentials = true;
    //上传进度事件
    xhr.upload.addEventListener("progress", function(result) {
        if (result.lengthComputable) {
            //上传进度
            var percent = (result.loaded / result.total * 100).toFixed(2);
        }
    }, false);

    xhr.addEventListener("readystatechange", function() {
        responseHandler(xhr, responseCallback);
    });

    xhr.send(data); //开始上传
}

    function responseHandler(xhr, responseCallback){
        var result = xhr;
        if (result.status !== 200) { //error
            downloadCallback(result.response);
            console.log('上传失败', result.status, result.statusText, result.response);
        }
        else if (result.readyState === 4) { //finished
            console.log('上传成功', result);
        }
    }

    function downloadCallback(returnData){
        $("#return_data").val(returnData);
    }

$("#submit.btn-block").bind("click", function () {
    alert('ok');
    download();
});
