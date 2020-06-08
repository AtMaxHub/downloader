<!DOCTYPE html>
<html>
<head lang="en">
    <meta charset="UTF-8"/>
    <title>m3u8 视频下载</title>
    <script type="text/javascript">
        function download1() {
            post();
        }
    </script>
</head>
<body>
    ${name}<br/>
    <form name="resource_prop" >
        m3u8_url: <input name="m3u8Url" value="" style="width: 100%" /> <br/>
        referer: <input name="referer" value="" /> <br/>
        <label><input name="Fruit" type="radio" value="" />h264 </label>
        <label><input name="Fruit" type="radio" value="" />acc </label>
    </form>

    <a href="#" onclick="download1()">download1</a><br/>
    <a href="#" onclick="download()">download</a><br/>
    <button onclick="download()">下载</button>
</body>
<script src="js/common.js" type="text/javascript"></script>
</html>