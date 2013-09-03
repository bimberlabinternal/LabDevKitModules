Ext4.define('LDK.data.CaseInsensitiveModel', {
    extend: 'Ext.data.Model',

    //note: alias this property, since Ext will auto-generate this field and it can lead to conflicts
    idProperty: '_internalId',

    constructor: function(){

        this.callParent(arguments);
    },

    //case-insensitive field lookup
    getCaseInsensitiveFieldMap: function(){
        if (!this.caseInsensitiveFieldMap){
            this.caseInsensitiveFieldMap = {};
            this.fields.each(function(f){
                if (f && f.name){
                    LDK.Assert.assertNotEmpty('LDK.data.LabKeyStore has fields without a name: ' + this.caseInsensitiveFieldMap[f.name.toLowerCase()] + ' / ' + f.dataIndex, f.name);
                    LDK.Assert.assertEmpty('LDK.data.LabKeyStore has fields of the same name with different case: ' + this.caseInsensitiveFieldMap[f.name.toLowerCase()] + ' / ' + f.dataIndex, this.caseInsensitiveFieldMap[f.name.toLowerCase()]);
                    this.caseInsensitiveFieldMap[f.name.toLowerCase()] = f.name;
                }
                else {
                    LDK.Utils.logToServer({
                        message: 'Field is either null or lacks a name: ' + (f ? f.name : '')
                    });
                }
            }, this);
        }

        return this.caseInsensitiveFieldMap;
    },

    setFields: function(){
        delete this.caseInsensitiveFieldMap;
        this.callParent(arguments);
    },

    resolveField: function(name){
        if (Ext4.isEmpty(name)){
            console.error('Unable to resolve field: ' + name);
            return;
        }

        var resolved = this.getCaseInsensitiveFieldMap()[name.toLowerCase()];
        if (!resolved){
            console.error('Unable to resolve field: ' + name);
        }
        if (resolved != name){
            console.log('differing field name: ' + name + '/' + resolved);
        }

        return resolved || name;
    },

    //case-insensitive
    get: function(name){
        return this.callParent([this.resolveField(name)]);
    },

    set: function(fieldName, newVal){
        var data;
        if (typeof fieldName == 'string'){
            data = {};
            data[fieldName] = newVal;
        }
        else {
            data = fieldName; //was called using object to set values
        }

        var newData = {};
        for (var name in data){
            newData[this.resolveField(name)] = data[name];
        }

        this.callParent([newData]);
    }
});