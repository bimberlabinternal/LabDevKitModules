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

    useDefaultPanel: false,

    initComponent: function(){
        this.renderData = this.renderData || {};
        this.renderData.title = this.title;
        this.title = null;

        Ext4.apply(this, {
            renderTpl: [
                '<div name="webpart" id="{id}-body" class="labkey-portal-container">',
                '<div class="panel ' + (this.useDefaultPanel ? 'panel-default' : 'panel-portal') + '">',
                '<div class="panel-heading">',
                    '<h3 class="panel-title pull-left" title="{title}">',
                        '<a name="{title}" class="labkey-anchor-disabled">',
                            '<span class="labkey-wp-title-text">{title}</span>',
                        '</a>',
                    '</h3>',
                    '<div class="clearfix"></div>',
                '</div>',
                '<div class="panel-body" id="{id}-innerDiv">',
                    '{%this.renderContainer(out,values);%}',
                '</div>',
                '</div>',
                '</div>'
            ],
            childEls: ['innerDiv']
        });

        this.callParent();
    },

    getTargetEl: function() {
        return this.innerDiv;
    }
});