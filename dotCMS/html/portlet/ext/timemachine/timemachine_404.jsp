<%@page import="com.liferay.portal.language.LanguageUtil"%>

<style>

	.content{position:fixed;left:50%;top:50%;margin:-200px 0 0 -300px;width:600px;background:#333;opacity:.85;color:#fff;padding:20px 20px 35px 20px;-moz-border-radius: 15px;-webkit-border-radius: 15px;-moz-box-shadow:0px 0px 15px #666;-webkit-box-shadow:0px 0px 15px #666;}
	.content h2{font-size:200%;}
	.content p{margin:0;}
	.content ul{margin:5px 0 25px 15px;padding:0 0 0 10px;list-style-position:outside; list-style:decimal;}
	.content li{list-style-position:outside; list-style:disc;}
	.content a{color:#fff;}
</style>
<div class="greyBg"></div>
<div class="wrapper">
	<div class="content">
		<h2><%=LanguageUtil.get(pageContext, "javax.portlet.title.TIMEMACHINE")%></h2>
		<p><%= LanguageUtil.get(pageContext, "TIMEMACHINE-404") %></p>

	</div>
</div>


