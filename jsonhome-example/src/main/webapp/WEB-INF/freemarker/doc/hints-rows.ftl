<#assign hints = resource.hints>
    <tr>
        <th>Allows</th>
        <td>
            <ul>
<#list hints.allows as allow>
                <li>${allow}</li>
</#list>
            </ul>
        </td>
        <td>The allowed http methods.</td>
    </tr>
    <tr>
        <th>Representations</th>
        <td>
            <ul>
<#list hints.representations as representation>
                <li>${representation}</li>
</#list>
            </ul>
        </td>
        <td>Available representations of the resource.</td>
    </tr>
    <tr>
        <th>Accept-Put</th>
        <td>
            <ul>
<#list hints.acceptPut as acceptPut>
                <li>${acceptPut}</li>
</#list>
            </ul>
        </td>
        <td>Available representations of the resource supported for PUT.</td>
    </tr>
    <tr>
        <th>Accept-Post</th>
        <td>
            <ul>
<#list hints.acceptPost as acceptPost>
                <li>${acceptPost}</li>
</#list>
            </ul>
        </td>
        <td>Available representations of the resource supported for POST.</td>
    </tr>
