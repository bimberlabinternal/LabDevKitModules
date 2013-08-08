/**
 * Designed to create an Ext4 container with the same outer border as a regular LabKey webpart.  It extends Ext Container,
 * and will support any configuration from this class.
 * @class LDK.panel.WebpartPanel
 * @cfg title The title of the webpart
 * @example &lt;script type="text/javascript"&gt;

    Ext4.create('LDK.panel.WebpartPanel', {
        title: 'Enter Data',
        items: [{
            xtype: 'form',
            items: [{
                xtype: 'textfield',
                fieldLabel: 'Sample Name'
            }],
            buttons: [{
                text: 'Submit'
            }],
            border: false
        }]
    }).render('example');

 &lt;/script&gt;
 &lt;div id='example'/&gt;
 */
Ext4.define('LDK.panel.WebpartPanel', {
    extend: 'Ext.container.Container',
    alias: 'widget.ldk-webpartpanel',
    layout: 'webpart',

    initComponent: function(){
        this.renderData = this.renderData || {};
        this.renderData.title = this.title;
        this.title = null;

        Ext4.apply(this, {
            renderTpl: [
                '<div id="{id}-body" class="ldk-wp">',
                    '<table id="{id}-table" class="labkey-wp"><tbody>',
                    '<tr class="labkey-wp-header">',
                    '<th class="labkey-wp-title-left">{title}</th>',
                    '<th class="labkey-wp-title-right">&nbsp;</th>',
                    '</tr><tr>',
                    '<td colspan=2 class="labkey-wp-body">',
                        '<div id="{id}-innerDiv">',
                        '{%this.renderContainer(out,values);%}',
                        '</div>',
                    '</td></tr></tbody></table>',
                '</div>'
                //for some reason having this el causes issues w/ IE8
                //'<div id="{id}-clearEl" class="', Ext4.baseCSSPrefix, 'clear" role="presentation"></div>',
            ],
            childEls: ['table', 'innerDiv']
        });

        this.callParent();
    },

    getTargetEl: function() {
        return this.innerDiv;
    }
});

/**
 * Slightly ugly.  Ext will not normally consider the WP borders when calculating layout.  Therefore
 * we override that calculation here and add an additional 50px;
 */
Ext4.define('LDK.layout.container.WebPart', {
    extend: 'Ext.layout.container.Auto',
    alias: 'layout.webpart',

    calculateContentSize: function (ownerContext, dimensions) {
        var orig = ownerContext.getProp('contentHeight');
        this.callParent(arguments);
        var height = ownerContext.getProp('contentHeight');
        //only append the extra padding if height changes
        if (orig != height){
            ownerContext.setProp('contentHeight', height + 50);
        }
    }
});