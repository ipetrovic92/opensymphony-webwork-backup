var webworkValidator = new ValidationClient("$!base/validation");
webworkValidator.onErrors = function(input, errors) {

	var table = input.parentNode.parentNode.parentNode;
	var form = input.form;

	clearErrorRows(table);
	clearErrorLabels(form);

	for (var fieldName in errors.fieldErrors) {
		if (form.elements[fieldName].touched) {
			for (var i = 0; i < errors.fieldErrors[fieldName].length; i++) {
				addError(form.elements[fieldName], errors.fieldErrors[fieldName][i]);
			}
		}
	}
}

function validate(element) {
    // mark the element as touch
    element.touched = true;
    var namespace = element.form.attributes['namespace'].nodeValue;
    var actionName = element.form.attributes['name'].nodeValue;
	webworkValidator.validate(element, namespace, actionName);
}

function clearErrorRows(table) {
    // clear out any rows with an "errorFor" attribute
    var rows = table.rows;
    var rowsToDelete = new Array();
    for(var i = 0; i < rows.length; i++) {
        var r = rows[i];
        if (r.getAttribute("errorFor")) {
            rowsToDelete.push(r);
        }
    }

    // now delete the rows
    for (var i = 0; i < rowsToDelete.length; i++) {
        var r = rowsToDelete[i];
        table.deleteRow(r.rowIndex);
    }
}

function clearErrorLabels(form) {
    // set all labels back to the normal class
    var elements = form.elements;
    for (var i = 0; i < elements.length; i++) {
        var e = elements[i];
        var cells = e.parentNode.parentNode.cells;
        if (cells && cells.length >= 2) {
            var label = cells[0].getElementsByTagName("label")[0];
            if (label) {
                label.setAttribute("class", "label");
            }
        }
    }

}


function addError(e, errorText) {
    try {
        // clear out any rows with an "errorFor" of e.id
        var row = e.parentNode.parentNode;
        var table = row.parentNode;
        var error = document.createTextNode(errorText);
        var tr = document.createElement("tr");
        var td = document.createElement("td");
        var span = document.createElement("span");
        td.align = "center";
        td.valign = "top";
        td.colSpan = 2;
        span.setAttribute("class", "errorMessage");
        span.appendChild(error);
        td.appendChild(span);
        tr.appendChild(td);
        tr.setAttribute("errorFor", e.id);;
        table.insertBefore(tr, row);

        // updat the label too
        var label = row.cells[0].getElementsByTagName("label")[0];
        label.setAttribute("class", "errorLabel");
    } catch (e) {
        alert(e);
    }
}