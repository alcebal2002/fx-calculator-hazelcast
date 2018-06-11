<html>
    <head>
    	<#if refreshPage>
    	    <meta http-equiv="refresh" content="10" />
    	</#if>
        <title>Monitor</title>
        <link href="/bootstrap-3.3.7-dist/css/bootstrap.css" rel="stylesheet" />
        <script src="/chart.js-2.7.2/dist/Chart.bundle.js"></script>
        <script src="/chart.js-2.7.2/dist/utils.js"></script>
		<style>
			* {
				box-sizing: border-box;
			}

			/* Create two equal columns that floats next to each other */
			.column {
				float: left;
				width: 50%;
				padding: 10px;
				height: 300px; /* Should be removed. Only for demonstration */
			}

			/* Clear floats after the columns */
			.row:after {
				content: "";
				display: table;
				clear: both;
			}
		</style>
    </head>
    <body>
	<#assign currentStatus = statusMap["status"]>
	
	
<div class="row">
	<div class="column" id="canvas-holder" style="width:40%">
		<canvas id="chart-area"></canvas>
	</div>
	<div class="column" style="width:60%">
	  <table class="table table-condensed">
		<thead>
		  <tr><th colspan="8"><i>Status: ${currentStatus}</i></th></tr>
		  <tr>
			<th>Status</th>
			<th>Address:Port</th>
			<th>Start Time</th>
			<th>End Time</th>
			<th>Elapsed Time</th>
			<th>Max Pool Size</th>
			<th># Processed</th>
			<th>Avg process time</th>
		  </tr>
		</thead>
		<tbody>
	<#assign totalExecuted = 0>
	<#assign totalWorkers = 0>
	<#assign totalThreads = 0>
	<#assign averageExecutionTime = 0>
	<#list monitorMap?values as workerDetail>
		<#assign totalExecuted = totalExecuted + workerDetail.totalExecutions>
		<#assign totalWorkers = totalWorkers + 1>
		<#assign totalThreads = totalThreads + workerDetail.poolMaxSize>
		<#assign averageExecutionTime = averageExecutionTime + (workerDetail.totalExecutions * workerDetail.avgExecutionTime)>
			<tr>
				<td>${workerDetail.activeStatusString}</td>
				<td>${workerDetail.inetAddres}:${workerDetail.inetPort}</td>
				<td>${workerDetail.startTimeString}</td>
				<td>${workerDetail.stopTimeString}</td>
				<td>${workerDetail.totalElapsedTime}</td>
				<td>${workerDetail.poolMaxSize}</td>
				<td>${workerDetail.totalExecutions}</td>
				<td>${workerDetail.avgExecutionTime}</td>
			</tr>
	</#list>
	<#if totalWorkers gt 0>
	  <#assign averageExecutionTime = averageExecutionTime / totalExecuted>
	</#if>

			<tr>
				<td><b>${totalWorkers}</b></td>
				<td colspan="4">&nbsp;</td>
				<td><b>${totalThreads}</b></td>
				<td><b>${totalExecuted}</b></td>
				<td><b>${averageExecutionTime}</b></td>
			</tr>
			<tr><td colspan="8">&nbsp;</td></tr>
		</tbody>
	  </table>
  </div>
</div>

<script>
	var config = {
		type: 'doughnut',
		data: {
			datasets: [{
				data: [
					<#list monitorMap?values as workerDetail>
					${workerDetail.totalExecutionsWithoutComma},
					</#list>
				],
				backgroundColor: [
					window.chartColors.red,
					window.chartColors.green,
					window.chartColors.yellow,
					window.chartColors.purple,
					window.chartColors.blue,
					window.chartColors.grey,
					window.chartColors.orange,
				],
				label: 'Workers Monitor'
			}],
			labels: [
				<#list monitorMap?values as workerDetail>
				'${workerDetail.inetAddres}:${workerDetail.inetPort}',
				</#list>
			]
		},
		options: {
		  elements: {
			  center: {
			  text: '${totalExecuted}',
			  color: '#36A2EB', //Default black
			  fontStyle: 'Helvetica', //Default Arial
			  sidePadding: 15 //Default 20 (as a percentage)
			}
		  }
		}
	};

	Chart.pluginService.register({
	  beforeDraw: function (chart) {
		if (chart.config.options.elements.center) {
		  //Get ctx from string
		  var ctx = chart.chart.ctx;

		  //Get options from the center object in options
		  var centerConfig = chart.config.options.elements.center;
		  var fontStyle = centerConfig.fontStyle || 'Arial';
		  var txt = centerConfig.text;
		  var color = centerConfig.color || '#000';
		  var sidePadding = centerConfig.sidePadding || 20;
		  var sidePaddingCalculated = (sidePadding/100) * (chart.innerRadius * 2)
		  //Start with a base font of 30px
		  ctx.font = "30px " + fontStyle;

		  //Get the width of the string and also the width of the element minus 10 to give it 5px side padding
		  var stringWidth = ctx.measureText(txt).width;
		  var elementWidth = (chart.innerRadius * 2) - sidePaddingCalculated;

		  // Find out how much the font can grow in width.
		  var widthRatio = elementWidth / stringWidth;
		  var newFontSize = Math.floor(30 * widthRatio);
		  var elementHeight = (chart.innerRadius * 2);

		  // Pick a new font size so it will not be larger than the height of label.
		  var fontSizeToUse = Math.min(newFontSize, elementHeight);

		  //Set font settings to draw it correctly.
		  ctx.textAlign = 'center';
		  ctx.textBaseline = 'middle';
		  var centerX = ((chart.chartArea.left + chart.chartArea.right) / 2);
		  var centerY = ((chart.chartArea.top + chart.chartArea.bottom) / 2);
		  ctx.font = fontSizeToUse+"px " + fontStyle;
		  ctx.fillStyle = color;

		  //Draw text in center
		  ctx.fillText(txt, centerX, centerY);
		}
	  }
	});
	
	window.onload = function() {
		var ctx = document.getElementById('chart-area').getContext('2d');
		window.myPie = new Chart(ctx, config);
	};
</script>
</body>
</html>