<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
        "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
    <title>Example Shop - REST API</title>
    <link href="${contextpath}/resources/css/jsonhome.css" rel="stylesheet" type="text/css">
</head>
<body>
    <h1>${resource.linkRelationType}</h1>

    <table>
        <thead>
            <tr>
                <th>Attribute</th>
                <th>Value</th>
                <th>Description</th>
            </tr>
        </thead>
        <tbody>
        <tr>
                <th>Rel</th>
                <td>${resource.linkRelationType}</td>
                <td>
<#list resource.documentation.description as doc>
                    ${doc}<br/>
</#list>
                </td>
        </tr>
        <tr>
                <th>Href</th>
                <td><a href="${resource.href}">${resource.href}</a></td>
                <td>The href link to the resource.</td>
        </tr>
<#include "hints-rows.ftl">
        </tbody>
        <tfoot>
        <tr>
            <td colspan="3">&nbsp;</td>
        </tr>
        </tfoot>
    </table>
</body>
</html>