/**
 * @param value
 * @param emptyText
 * @param editable
 * @event update
 */
Ext4.define('LDK.field.EditInPlaceElement', {
    extend: 'Ext.container.Container',
    alias: 'widget.ldk-editinplaceelement',

    initComponent: function(){
        Ext4.apply(this, {
            items: [this.getDisplayConfig()]
        });

        this.addEvents('update');
        this.callParent(arguments);
    },

    getValue: function(){
        return this.value;
    },

    getDisplayConfig: function(){
        return {
            html: Ext4.isEmpty(this.value) ? this.emptyText : Ext4.util.Format.htmlEncode(this.value),
            border: false,
            cls: Ext4.isEmpty(this.value) ? 'labkey-edit-in-place-empty' :'',
            style: 'white-space: pre-wrap;',
            listeners: {
                scope: this,
                single: true,
                afterrender: function(item){
                    if (!this.editable)
                        return;

                    item.getEl().on('click', this.enterEditMode, this);
                    Ext4.defer(this.appendEditIcon, 100, this);
                }
            }
        }
    },

    appendEditIcon: function(){
        this.editIcon = Ext4.getBody().createChild({
            tag: 'div',
            cls: 'labkey-edit-in-place-icon',
            title: 'Click to Edit'
        });
        this.editIcon.anchorTo(this.getEl(), 'tr-tr');
        this.editIcon.on("mouseover", function(){
            this.editIcon.addCls("labkey-edit-in-place-icon-hover");
        }, this);
        this.editIcon.on("mouseout", function(){
            this.editIcon.removeCls("labkey-edit-in-place-icon-hover");
            this.editIcon.removeCls("labkey-edit-in-place-icon-mouse-down");
        }, this);
        this.editIcon.on("mousedown", function(){
            this.editIcon.addCls("labkey-edit-in-place-icon-mouse-down");
        }, this);
        this.editIcon.on("mouseup", function(){
            this.editIcon.removeCls("labkey-edit-in-place-icon-mouse-down");
        }, this);
        this.editIcon.on("click", this.enterEditMode, this);
    },

    enterEditMode: function(){
        this.removeAll();
        if (this.editIcon)
            this.editIcon.destroy();

        var width = this.getWidth();

        this.add({
            xtype: 'textarea',
            itemId: 'textbox',
            width: width - 10,
            value: this.value,
            enableKeyEvents: true,
            grow: true,
            listeners: {
                scope: this,
                blur: this.completeEdit,
                afterrender: function(item){
                    item.focus();
                },
                specialkey: function(field, event){
                    if (event.getKey() === Ext4.EventObject.ESC)
                        this.cancelEdit();
                }
            }
        });
    },

    completeEdit: function(){
        var changed = false;
        var oldText = this.value;
        var newText = this.down('#textbox').getValue();

        this.value = newText;
        this.removeAll();
        this.add(this.getDisplayConfig());

        if (oldText !== newText){
            this.fireEvent('update', this, newText, oldText)
        }
    },

    cancelEdit: function(){
        this.removeAll();
        this.add(this.getDisplayConfig());
    }
});
