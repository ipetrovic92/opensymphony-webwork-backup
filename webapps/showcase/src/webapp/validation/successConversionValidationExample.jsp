<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@taglib prefix="ww" uri="/webwork" %>    
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Showcase - Validation - Conversion Validation </title>
</head>
<body>

	Age:<ww:property value="%{age}" /><br/>
	Bean Age:<ww:property value="%{bean.age}" /><br/>

</body>
</html>