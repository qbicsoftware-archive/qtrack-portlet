/**
 * Connects JavaScript with Vaadin
 */
window.com_vaadin_ui_LineChart = function() {

    // Create the component
    var lineChart = new myGraph.LineChart(this.getElement());

    // Handle changes from the server-side
    this.onStateChange = function() {
       console.log(this.getState().data)
       lineChart.setData(this.getState().data, this.getState().selectedOptions);
    };
};
