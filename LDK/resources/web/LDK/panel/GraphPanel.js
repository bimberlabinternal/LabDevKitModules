/**
 * EXPERIMENTAL.  This is designed to be a higher lever interface for the LK charting API, and simplify rendering a chart inside Ext4 panels.
 * You should be able to provide either selectRow results, or a queryConfig, in addition to chart-specific configuration.  It will generate
 * a panel with reasonable UI based on the metadata of that particular query.
 * @param {Object} The results of a selectRows() call
 * @param {String} title
 * @param {String} xLabel
 * @param {String} yLabel
 * @param {String} xField
 * @param {Array} grouping
 * @param {Array} layers
 */
Ext4.define('LDK.panel.GraphPanel', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.ldk-graphpanel',

    initComponent: function(){
        Ext4.apply(this, {
            minHeight: 50,
            border: false,
            items: [{
                xtype: 'ldk-contentresizingpanel',
                itemId: 'graphPanel',
                border: false
            }]
        });

        Ext4.applyIf(this, {
            style: 'margin-bottom: 10px;'
        });

        this.callParent(arguments);

        this.renderTarget = this.down('#graphPanel').renderTarget;
    },

    afterRender: function(){
        this.callParent(arguments);

        var config = this.getPlotConfig();
        if (!config.data || !config.data.length){
            this.add({
                border: false,
                html: 'There is no data to plot'
            });

            return;
        }

        var plot = new LABKEY.vis.Plot(config);
        this.appendLayers(plot);
        plot.render();

    },

    getPlotConfig: function(){
        var config = this.plotConfig || {};

        return {
            renderTo: this.renderTarget,
            labels: {
                x: {value: config.xLabel},
                y: {value: config.yLabel},
                main: {value: config.title}
            },
            height: config.height || 400,
            width: config.width || 900,
            clipRect: true,
            rendererType: 'd3',
            data: LDK.QueryHelper.getRowMapsFromSelectRows(this.plotConfig.results),
            aes: {
                x: function(row){
                    return row[config.xField] ? row[config.xField] : 0;
                },
                color: function(row){
                    return row[config.grouping]
                },
                group: function(row){
                    return row[config.grouping]
                },
                shape: function(row){
                    return row[config.grouping]
                }
            },
            scales: this.getScaleConfig(),
            success: function(){
                console.log('success!');
            }
        };
    },

    getScaleConfig: function(){
        var config = this.plotConfig || {};

        return Ext4.apply({
            x: {
                scaleType: 'continuous',
                trans: 'linear',
                tickFormat: this.getTickFormatFnForField(this.plotConfig.xField)
            },
            y: {
                scaleType: 'continuous',
                trans: 'linear',
                tickFormat: this.getTickFormatFn('float')
            },
            shape: {
                scaleType: 'discrete'
            }
        }, config.scales);
    },

    getTickFormatFnForField: function(fieldName){
        var meta = this.findMetadata(fieldName);
        if(meta.extFormatFn && !Ext4.isFunction(meta.extFormatFn)){
            try {
                meta.extFormatFn = eval(meta.extFormatFn);

            }
            catch (error) {
                console.error(error);
            }
        }
        return this.getTickFormatFn(meta.jsonType, meta);
    },

    getTickFormatFn: function(jsonType, meta){
        meta = meta || {};
        switch (jsonType){
            case 'date':
                //NOTE: we could attempt to format based on the datetime format provided by this column, but it rarely makes sense
                return function(value){return value ? Ext4.Date.format(new Date(value), LABKEY.extDefaultDateFormat) : null};
                break;
            case 'int':
                if (meta.extFormatFn)
                    return function(value){return meta.extFormatFn(value)};
                else
                    return function(value){return Ext4.util.Format.round(value, 0)};
                break;
            case 'float':
                if (meta.extFormatFn)
                    return function(value){return meta.extFormatFn(value)};
                else
                    return function(value){return Ext4.util.Format.round(value, 2)};
                break;
            default:
                return function(value){return value};
        }
    },

    appendLayers: function(plot){
        for (var i=0;i<this.plotConfig.layers.length;i++){
            this.appendLayer(plot, this.plotConfig.layers[i]);
        }
    },

    appendLayer: function(plot, layerConfig){
        //TODO: automatically configure based on type, like line, bar, etc.
        var meta = this.findMetadata(layerConfig.y);
        var cfg = {
            geom: new LABKEY.vis.Geom.Point({size: 5}),
            name: layerConfig.name || meta.caption,
            aes: {
                y: function(row){
                    return row[layerConfig.y]
                }
            }
        };
        if (layerConfig.hoverText)
            cfg.aes.hoverText = layerConfig.hoverText;

        plot.addLayer(new LABKEY.vis.Layer(cfg));

        plot.addLayer(new LABKEY.vis.Layer({
            geom: new LABKEY.vis.Geom.Path({size: 3, opacity: .2}),
            name: layerConfig.name || meta.caption,
            aes: {
                y: function(row){
                    return row[layerConfig.y]
                }
            }
        }));
    },

    findMetadata: function(name){
        name = name.toLowerCase();
        var meta = this.plotConfig.results.metaData;

        for (var i=0;i<meta.fields.length;i++){
            if(meta.fields[i].name.toLowerCase() == name)
                return meta.fields[i];
        }
    }
});