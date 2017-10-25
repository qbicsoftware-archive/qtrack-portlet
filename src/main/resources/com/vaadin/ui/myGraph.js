/**
 * Created by caspar on 04.07.17.
 */
// Define the namespace

var myGraph = myGraph || {};

myGraph.LineChart = function (element) {

    var data;
    var selectedOptions;

    var parseTime = d3.timeParse("%d-%b-%y");

    // this function gets called whenever the data for myGraph has been set
    this.setData = function (dataString, userSelectedOptions) {
    
        // parse the input represented by json 
        var dat = JSON.parse(dataString);
        selectedOptions = JSON.parse(userSelectedOptions);


        // TODO: check if this is really necessary
        dat.forEach(function (d) {
            d.usersteps = +d.steps;
            d.averagesteps = +d.average[0];
            d.millisstart = +d.startMillis;
        });

        // sort the data by time
        data = dat.sort(function(a,b){
            return a.millisstart-b.millisstart;
        });

        console.log(data);
        
        // draw the data
        updateWindowVariables();
        drawChart(data);
    };

    // some variables for the window sizes:
    var w,d,e,windowG,windowX,windowY,vaadin_menu_width,margin,width,height,svg,g,adjusted_height,x,y;

    // variables representing the lines for the user steps and the avg steps
    var userStepsLine, avgStepsLine;

    /*
     * this function updates the variables related to the window 
     */
    function updateWindowVariables() {

        // get the current window sizes
        w = window,
            d = document,
            e = d.documentElement,
            windowG = d.getElementsByTagName('body')[0],
            windowX = w.innerWidth || e.clientWidth || windowG.clientWidth,
            windowY = w.innerHeight|| e.clientHeight|| windowG.clientHeight;

        // the vaadin menu sizes (left and top), are depending on the window width
        if (windowX > 1000) {
            vaadin_menu_width = 200;
            vaadin_top_bar_height = 65;
        } else if (windowX <= 1000 && windowX > 600) {
            vaadin_menu_width = 80;
            vaadin_top_bar_height = 65;
        } else {
            vaadin_menu_width = 0;
            vaadin_top_bar_height = 65+37;
        }

        // set the margin, the width and the height accordingly
        margin = {top: 10, right: 150, bottom: 30, left: 50},
            width = windowX - margin.left - margin.right - vaadin_menu_width,
            height = windowY - margin.top - margin.bottom - vaadin_top_bar_height;

        // remove previously drawn elements
        d3.select("svg").remove();

        // create the svg with the corresponding size
        svg = d3.select(element).append("svg:svg").attr("width", width+margin.right).attr("height",
                                                                                    height),
            g  = svg.append("g").attr("transform", "translate(" + margin.left + "," + margin.top + ")");

        // adjust the height for the range of the y axis
        adjusted_height = height-margin.top-margin.bottom;

        // set the ranges for the axis
        x = d3.scaleLinear().range([10, width]);
        y = d3.scaleLinear().range([adjusted_height, 0]);

        // define the 1st line
        userStepsLine = d3.line()
            .x(function(d) { return x(d.millisstart); })
            .y(function(d) { return y(d.usersteps); });

        // define the 2nd line
        avgStepsLine = d3.line()
            .x(function(d) { return x(d.millisstart); })
            .y(function(d) { return y(d.averagesteps); });
    }


    /*
     * updates the view by updating the window variables and redrawing the chart afterwards
     */
    function updateView(){

        // update width, height, etc. for the window
        updateWindowVariables();

        // draw the data
        drawChart(data);
    }


    /*
     * helper function to add a listener to the corresponding event
     * code based on:
     * https://stackoverflow.com/questions/641857/javascript-window-resize-event/3150139#3150139
     */
    var addEvent = function(object, type, callback) {
        if (object == null || typeof(object) == 'undefined') return;
        if (object.addEventListener) {
            object.addEventListener(type, callback, false);
        } else if (object.attachEvent) {
            object.attachEvent("on" + type, callback);
        } else {
            object["on"+type] = callback;
        }
    };

    // resizeWindow gets called after 200ms after the window has been resized
    var timeoutForResizeWindow;
    addEvent(window, "resize", function(event) {

        console.log('resized');
        clearTimeout(timeoutForResizeWindow);
        timeoutForResizeWindow = setTimeout(updateView, 200);
    });


    /*
     * calculates the number of ticks to use for the x axis
     * param n: number of measurements
     */
    function calculateNumberOfTicks(n) {

        // calculate the number of ticks we can display
        var tickLength = 65;
        var tickPadding = 10;
        var numberOfTicks = Math.floor(width/(tickLength+tickPadding));

        // deal with a width too small or too large
        if (numberOfTicks < 1) {
            numberOfTicks = 1;
        } else if (numberOfTicks > n) {
            numberOfTicks = n;
        }

        console.log("there are" + numberOfTicks + " ticks fitting in a width of " + width)
        console.log("every " + Math.floor(n/numberOfTicks) + "days")

        return Math.floor(n/numberOfTicks);
    }

    /*
     * this function returns the dates for the x axis labels (every x-th date, where x depends on the width of the
     * window)
     */
    function getDatesForXAxis(x) {

        // get all dates between the first and the last measurement
        var now = new Date(x.domain()[1]);
        var daysOfMeasurements = [];
        for (var d = new Date(x.domain()[0]); d <= now; d.setDate(d.getDate() + 1)) {
            daysOfMeasurements.push(new Date(d));
        }

        // we are only interested in each xth day
        var everyXthDay = [];
        var delta = calculateNumberOfTicks(daysOfMeasurements.length);   // calculate how many x labels we can display
        for (i = 0; i < daysOfMeasurements.length; i=i+delta) {
            everyXthDay.push(daysOfMeasurements[i]);
        }

        console.log(everyXthDay);
        return everyXthDay;
    }


    /**
     * creates the tooltip displaying the date for a certain datapoint
     * @param dat: array of objects holding the data
     * @param l: which data point to create the tooltip for
     * @returns: string representing the tooltip for the data point, e.g. 20/10/2017: \n Average Steps: 8057 \n
                                                                                         Your Steps: 12345
     */
    function createDateTooltipForDataPoint(dat, l) {

        var timeFormat = d3.timeFormat("%Y-%m-%d");
        if (Math.round(dat[l].averagesteps) > Math.round(dat[l].steps)) {
            return timeFormat(dat[l].startMillis);
/*
            return timeFormat(dat[l].startMillis) + ":\n "
                                         + "Average Steps: " + Math.round(dat[l].averagesteps) + "\n"
                                         + "Your Steps: " + Math.round(dat[l].steps);
*/
        } else {
            return timeFormat(dat[l].startMillis);
/*
            return timeFormat(dat[l].startMillis) + ":\n "
                                         + "Your Steps: " + Math.round(dat[l].steps) + "\n"
                                         + "Average Steps: " + Math.round(dat[l].averagesteps);
*/
        }
    }


    /*
     * draws the two lines, the dots and adds the scales and the legend
     * @param dat: the data to plot (array of objects containing the avg steps, user steps and date in milliseconds)
     */
    function drawChart(dat) {

        console.log(dat);
        // Scale the range of the data
        x.domain(d3.extent(dat, function(d) { return d.millisstart; }));
        y.domain([0, d3.max(dat, function(d) {return Math.max(d.usersteps, d.averagesteps); })]);

        // Add the user steps
        g.append("path")
            .data([dat])
            .attr("d", userStepsLine)
            .attr("stroke", selectedOptions.colorForUserSteps)
            .attr("fill", "none")
            .attr("stroke-width", "3px");

        // add the circles to select the tooltips
        svg.selectAll("dot")
            .data(dat)
            .enter().append("circle")
            .attr("r", 5)
            .attr("cx", function(d) { return x(d.startMillis); })
            .attr("cy", function(d) { return y(d.steps); })
            .attr("transform", "translate(50,12)")
            .attr("fill", selectedOptions.colorForUserSteps)
            .attr("id", function(d,i) { return "user_"+i; })
                 .append("svg:title")
                      .text(function(d, i) { return createDateTooltipForDataPoint(dat, i)});


        // Add the average steps
        g.append("path")
            .data([dat])
            .attr("d", avgStepsLine)
            .attr("stroke-width", "3px")
            .attr("fill", "none")
            .attr("stroke", selectedOptions.colorForAvgSteps);

        var timeFormat = d3.timeFormat("%Y-%m-%d");

        // add the circles to select the tooltips
        svg.selectAll("dot")
            .data(dat)
            .enter().append("circle")
            .attr("r", 5)
            .attr("cx", function(d) { return x(d.startMillis); })
            .attr("cy", function(d) { return y(d.averagesteps); })
            .attr("transform", "translate(50,12)")
            .attr("fill", selectedOptions.colorForAvgSteps)
            .attr("id", function(d,i) { return "avg_"+i; })
                 .append("svg:title")
/*                      .text(function(d, i) { return timeFormat(dat[i].startMillis) + ":\n "
                      + Math.round(dat[i].averagesteps) + " steps"; });*/
                      .text(function(d, i) { return createDateTooltipForDataPoint(dat, i)});


        /**
         * creates the tooltip text elements which are displayed when the user is hovering over the dots
         * @param coords: object holding the x and y coords for the text element e.g. {"cx": 10, "cy": 50}
         */
        function createTooltipTextElements(coords, isUserSelected) {

            var textToDisplay = {"selected": "", "notSelected": ""};
            if (isUserSelected) {
                textToDisplay.selected = "Your Steps: ";
                textToDisplay.notSelected = "Average Steps: ";
            } else {
                textToDisplay.selected = "Average Steps: ";
                textToDisplay.notSelected = "Your Steps: ";
            }

            // add the text for the currently selected dot
            svg.append("text")
                .attr("dy", ".35em")
                .attr("y", coords.cy+10)
                .attr("dx", coords.cx+30)
                .attr("text-anchor", "end")
                .style("fill", "black")
                .style("font-weight", "bold")
                .style("font-size", "large")
                .attr("id", "tooltip1")
                .text(textToDisplay.selected + "" + Math.round(coords.selectedSteps));

            // add the text for the other data point at the same time
            svg.append("text")
                .attr("dy", ".35em")
                .attr("y", coords.cy_other+10)
                .attr("dx", coords.cx+30)
                .attr("text-anchor", "end")
                .style("fill", "black")
                .style("font-weight", "bold")
                .style("font-size", "large")
                .attr("id", "tooltip2")
                .text(textToDisplay.notSelected + "" + Math.round(coords.notSelectedSteps));

        }

        /**
         * removes the element with the elementId from DOM
         * @param: elementId: the Id of the element to remove
         */
        function removeElementFromDOM(elementId) {

            var tooltipElem = document.getElementById(elementId);
            if (tooltipElem != null) {
                tooltipElem.parentElement.removeChild(tooltipElem);
            }
        }

        // deals with asynchronity of javascript
        /**
         * @param k: event listener are added to the k-th data point
         */
        function addEventListenerToDataPoint(k) {

            // TODO: https://stackoverflow.com/questions/40415144/how-to-externally-trigger-d3-events
            // idea: trigger mouseover event for both circles at the same time

            var event = document.createEvent('Event');
            event.initEvent('mouseover', true, true);

            document.getElementById("avg_"+k).addEventListener("mouseover", function() {
                console.log("mouse over avg" + k);
                console.log(x(dat[k].startMillis));
                console.log(y(dat[k].averagesteps));
                var isUserSelected = false;
                createTooltipTextElements({"cx": x(dat[k].startMillis), "cy": y(dat[k].averagesteps),
                                           "cy_other": y(dat[k].steps), "selectedSteps": dat[k].averagesteps,
                                           "notSelectedSteps": dat[k].steps}, isUserSelected);
            });

            document.getElementById("user_"+k).addEventListener("mouseover", function() {
                console.log("mouse over user" + k);
                console.log(x(dat[k].startMillis));
                console.log(y(dat[k].steps));
                var isUserSelected = true;
                createTooltipTextElements({"cx": x(dat[k].startMillis), "cy": y(dat[k].steps),
                                           "cy_other": y(dat[k].averagesteps), "selectedSteps": dat[k].steps,
                                           "notSelectedSteps": dat[k].averagesteps}, isUserSelected);
            });

            // when user stops hovering over a dot ..
            document.getElementById("avg_"+k).addEventListener("mouseout", function() {
                // .. delete the tooltip
                removeElementFromDOM("tooltip1");
                removeElementFromDOM("tooltip2");
            });

            // when user stops hovering over a dot ..
            document.getElementById("user_"+k).addEventListener("mouseout", function() {
                // .. delete the tooltip
                removeElementFromDOM("tooltip1");
                removeElementFromDOM("tooltip2");
            });

        }

        // add the event listeners for the tooltips to the data points 
        for (var k = 0; k < dat.length; k++) {
            addEventListenerToDataPoint(k);
        }

        // add the tooltips to the circles
/*
        var timeFormat = d3.timeFormat("%Y-%m-%d");
        for (var i = 0; i < dat.length; i++) {
            var title = document.createElementNS('http://www.w3.org/2000/svg', 'title');
            title.textContent = timeFormat(dat[i].startMillis) + ":\n " + Math.round(dat[i].averagesteps) + " steps";
            document.getElementById("avg_"+i).appendChild(title);
        }
*/


        // Add the X Axis
        g.append("g")
            .attr("transform", "translate(0," + adjusted_height + ")")
            .call(d3.axisBottom(x)
            .tickFormat(d3.timeFormat("%Y-%m-%d"))
            .tickValues(getDatesForXAxis(x))
            );

        // Add the Y Axis
        g.append("g")
            .call(d3.axisLeft(y));

        /*
         * Add legend
         */

        // add legend for average steps
        svg.append("text")
            .attr("dy", ".35em")
            .attr("y", 70)
            .attr("dx", width+150)
            .attr("text-anchor", "end")
            .style("fill", selectedOptions.colorForAvgSteps)
            .text("The others");

        svg.append('rect')
                .attr('width', 15)
                .attr('height', 15)
                .attr("y", 70-7.5)
                .attr("x", width+60)
                .style('fill', selectedOptions.colorForAvgSteps);

        // add legend for user steps
        svg.append("text")
            .attr("dy", ".35em")
            .attr("y", 50)
            .attr("dx", width+105)
            .attr("text-anchor", "end")
            .style("fill", selectedOptions.colorForUserSteps)
            .text("You ");

        svg.append('rect')
                .attr('width', 15)
                .attr('height', 15)
                .attr("y", 50-7.5)
                .attr("x", width+60)
                .style('fill', selectedOptions.colorForUserSteps);
    }

};