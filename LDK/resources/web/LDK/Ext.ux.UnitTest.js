/**
 * @class Ext.ux.UnitTest
 * A Unit Testing framework designed with an ExtJS user interface
 * @namespace Ext.ux
 * @author Arthur Kay (art *AT* akawebdesign *DOT* com)
 */
Ext.ux.UnitTest = {
    /**
     * @method
     * Builds the Unit Test UI
     */
    init: function() {
        var applicationPanel = new Ext.Panel({
            layout: 'border',
            height: 600,
            //autoHeight: true,
            items: [{
                    xtype: 'panel',
                    region: 'east',
                    //autoHeight: true,
                    height: '100%',
                    collapsible: true,
                    title: 'Statistics',
                    width: 500,
                    items: Ext.ux.UnitTest.Chart.chart
                },
                {
                    xtype: 'grid',
                    region: 'center',
                    id: 'test-grid',
                    autoExpandColumn: 'testName',
                    view: new Ext.grid.GroupingView({
                        forceFit: true,
                        startCollapsed: true,
                        groupTextTpl: '{text} ({[values.rs.length]} {[values.rs.length > 1 ? "Tests" : "Test"]})'
                    }),
                    columns: [{
                            dataIndex: 'testGroup',
                            header: 'Group',
                            hidden: true,
                            id: 'testGroup',
                            width: 300,
                            groupRenderer: Ext.createDelegate(function(newValue, unused, rowRecord, rowIndex, colIndex, dataStore){
                                var groupCls = '';
                                var pending = false;
                                dataStore.each(function(rec) {
                                    if (newValue === rec.data.testGroup) {
                                        if (!rec.data.testResult) {
                                            if (rec.data.testStatus === 'Pending...') {
                                                pending = true;
                                            }
                                            else {
                                                groupCls = 'failed-test-group';
                                                var view = Ext.getCmp('test-grid').getView();
                                                view.toggleGroup(view.getGroupId(rec.data.testGroup), true);
                                            }
                                            return false;
                                        }
                                    }
                                });

                                if (!pending && groupCls !== 'failed-test-group') {
                                    groupCls = 'passed-test-group';
                                }

                                return String.format('<span class="{0}">&nbsp;{1}&nbsp;</span>', groupCls, newValue);
                            }, this)
                        },
                        {
                            dataIndex: 'testName',
                            header: 'Name',
                            id:'testName',
                            width: 300
                        },
                        {
                            dataIndex: 'testStatus',
                            header: 'Test Status',
                            id: 'testStatus',
                            renderer: function(val, cell, record) {

                                if (val == 'Test passed.') {
                                    return '<span style="color: #090;">' + val + '</span>';
                                }
                                else if (val == 'Pending...') {
                                    return '<span style="color: #009;">' + val + '</span>';
                                }
                                else {
                                    return '<span style="color: #900;">' + val + '</span>';
                                }
                            },
                            width: 300
                        }
                    ],
                    height:350,
                    store: Ext.ux.UnitTest.Store,
                    stripeRows: true,
                    tbar: [{
                            xtype: 'button',
                            text: 'Start Unit Tests',
                            iconCls: 'x-tbar-loading',
                            handler: function() {
                                if (!Ext.ux.UnitTest.Store.getCount() > 0) {
                                    Ext.Msg.alert(
                                        'Error',
                                        'No tests have been loaded.'
                                    );
                                }
                                else {
                                    for (var i=0; i<Ext.ux.UnitTest.TestArray.length; i++) {
                                        var currentTest = Ext.ux.UnitTest.TestArray[i];

                                        currentTest.beginTest();
                                    }
                                }
                                Ext.getCmp('test-grid').getView().refresh();
                            }
                        }
                    ],
                    bbar: {
                        items: [{
                            text: 'Total tests:' + Ext.ux.UnitTest.Store.getCount()
                        }]
                    },
                    title:'Unit Tests'
                }
            ]        
        });
        applicationPanel.render('unitTest');
        Ext.ux.UnitTest.Chart.chart.show();
    }
};


/**
 * @class Ext.ux.UnitTest.TestArray
 * The master list of unit test instances.
 * @namespace Ext.ux.UnitTest
 * @singleton
 */
Ext.ux.UnitTest.TestArray = [];


/**
 * @class Ext.ux.UnitTest.Store
 * The grid's data store which displays the name and current status of each unit test object.
 * @namespace Ext.ux.UnitTest
 * @singleton
 */
Ext.ux.UnitTest.Store = new Ext.data.GroupingStore({
    fields: [ 
        'testName', 
        'testGroup', 
        'testResult', 
        'testStatus' 
    ],
    groupField: 'testGroup'
});

Ext.ux.UnitTest.Record = Ext.data.Record.create([
    { name: 'testName' },
    { name: 'testGroup' },
    { name: 'testResult' },
    { name: 'testStatus' }
]);


Ext.ux.UnitTest.Chart = new function() {
    this.addTest = function() {
        var numPendingTests = this.store.getAt(0).get('total');
        this.store.getAt(0).set('total', numPendingTests+1);
    };

    this.updatePass = function() {
        var numPendingTests = this.store.getAt(0).get('total');
        this.store.getAt(0).set('total', numPendingTests-1);

        var numPassingTests = this.store.getAt(1).get('total');
        this.store.getAt(1).set('total', numPassingTests+1);
    };

    this.updateFail = function() {
        var numPendingTests = this.store.getAt(0).get('total');
        this.store.getAt(0).set('total', numPendingTests-1);

        var numFailingTests = this.store.getAt(2).get('total');
        this.store.getAt(2).set('total', numFailingTests+1);
    };

    this.store = new Ext.data.JsonStore({
        fields: [ 'status', 'total' ],
        data: [
            { status: 'Pending', total: 0 },
            { status: 'Passed', total: 0 },
            { status: 'Failed', total: 0 }
        ]
    });

    this.chart = new Ext.chart.PieChart({
        categoryField: 'status',
        dataField: 'total',
        height: 500,
        width: 500,
        chartStyle: {
            legend: { display: 'right' }
        },
        store: this.store,
        series: [{
            style: { colors: ['#0000BB', '#00BB00', '#BB0000'] }
        }]
    });
};


/**
 * @class Ext.ux.UnitTest.Test
 * @namespace Ext.ux.UnitTest
 * @extends Ext.Component
 * @cfg {string} testName The name of this test
 * @cfg {string} testGroup The group to which this test will be associated
 * @cfg {function} testFunction The method executed when this test is run
 */
Ext.ux.UnitTest.Test = Ext.extend(Ext.Component, {
    /**
     * @property
     * @type integer
     * @default null
     */
    id: null,
    
    /**
     * @property
     * @type string
     * @default null
     */
    failValue: null,

    /**
     * @property
     * @type string
     * @default null
     */
    testName: null,

    /**
     * @property
     * @type string
     * @default null
     */
    testGroup: null,

    /**
     * @property
     * @type boolean
     * @default false
     */
    testResult: false,

    /**
     * @property
     * @type string
     * @default null
     */
    testStatus: null,

    /**
     * @method
     * @description The code performing the actual unit test. Takes one parameter "testObj", which is "this" Ext.ux.UnitTest.Test object.
     * @return string "Pass" or anything else as a failure message
     */
    testFunction: null,

    /**
     * @property
     * @type Ext.data.Record
     * @default null
    */
    unitTestRecord: null,

    constructor: function() {
        Ext.ux.UnitTest.Test.superclass.constructor.apply(this, arguments);

        this.testStatus = 'Pending...';

        this.unitTestRecord = new Ext.ux.UnitTest.Record({
            testName: this.testName,
            testGroup: this.testGroup,
            testStatus: this.testStatus,
            testResult: this.testResult
        });

        this.id = Ext.ux.UnitTest.TestArray.length;

        Ext.ux.UnitTest.Store.add(this.unitTestRecord);
        Ext.ux.UnitTest.TestArray.push(this);

        //update the chart
        Ext.ux.UnitTest.Chart.addTest();
    },

    /**
      * @method
      * @description Updates the testStatus property.
      * @param {string} status
     */
    updateStatus: function(status) {
        this.testStatus = status;
        this.unitTestRecord.set('testStatus', status);
        this.unitTestRecord.commit();
    },

    /**
     * @method
     *  @description Begins the test function for this Unit Test.
     */
    beginTest: function() {
        try {
            this.testFunction();
            if(this.isComplete === false)
                return;

            if (this.failValue === null) {
                this.markTestAsPassed();
            }
        }
        catch (err) {
            this.failValue = err;
            this.markTestAsFailed();
        }
    },

    /**
      * @method
      * @description Marks the Unit Test as "Test failed: ..."
    */
    markTestAsFailed: function() {
        this.updateStatus('Test failed: ' + this.failValue);
        this.testResult = false;
        this.unitTestRecord.set('testResult', false);
        Ext.ux.UnitTest.Chart.updateFail();
    },

    /**
     * @method
     * @description Marks the Unit Test as "Test passed."
    */
    markTestAsPassed: function() {
        this.updateStatus('Test passed.');
        this.testResult = true;
        this.unitTestRecord.set('testResult', true);
        Ext.ux.UnitTest.Chart.updatePass();
    },


    /**
      * @method
      * @description Creates a string, formatted to look like a GUID. Values are not stored, so this GUID should not be used for anything that requires a unique GUID.
      * @return string A GUID
    */
    generateGuid: function() {
        var guid = '';

        var characterSet = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz'.split('');

        function randomDigit() {
            var x = Math.round(Math.random()*61);
            var y = characterSet[x];

            return y;
        }

        //GUID format is 8-4-4-4-12
        for (var i=0; i<36; i++) {
            
            if (i == 8 || i == 13 || i == 18 || i == 23) {
                guid += '-';
            }
            else {
                guid += randomDigit();
            }
        }

        return guid;
    },
    

    /**
     * @method
     * @description Tests an input value to see if it is a GUID. If false, the provided error message is bubbled up to the UI.
     * @param {any} inputValue
     * @param {string} errorMsg The message to display in the Unit Test UI.
     */
    assertIsGuid: function(inputValue, errorMsg) {
        var guidRegEx = new RegExp('([A-Z0-9]{8}-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{12})', ["i"]);

        var result = guidRegEx.exec(inputValue);

        if (result !== null) { 

        }
        else {
            this.failValue = errorMsg;
            this.markTestAsFailed();
        }
    },

    /**
     * @method
     * @description Causes the test to wait for a specified number of seconds
     * @param {callback} Callback function to be executed after the wait.
     * @param {integer} milliseconds The number of milliseconds to wait.
     */
    wait: function(callback, milliseconds) {
        var waiting = setTimeout(function() {
            callback();
        }, milliseconds);
    },

    /**
     * @function
     * @description Compares a value against boolean TRUE. If values do not match, the provided error message is bubbled up to the UI.
     * @param {any} actual
     * @param {string} errorMsg The message to display in the Unit Test UI.
    */
    assertTrue: function(actual, errorMsg) {
        if (actual !== true) { 
            this.failValue = errorMsg;
            this.markTestAsFailed();
        }
    },

    /**
     * @function
     * @description Compares a value against boolean FALSE. If values do not match, the provided error message is bubbled up to the UI.
     * @param {any} actual
     * @param {string} errorMsg The message to display in the Unit Test UI.
    */
    assertFalse: function(actual, errorMsg) {
        if (actual !== false) { 
            this.failValue = errorMsg;
            this.markTestAsFailed();
        }
    },

    /**
     * @function
     * @description Compares two values for equality based on both value and type. If values do not match, the provided error message is bubbled up to the UI.
     * @param {any} actual
     * @param {any} expected
     * @param {string} errorMsg The message to display in the Unit Test UI.
    */
    assertEquals: function(actual, expected, errorMsg) {
        if (actual !== expected) { 
            this.failValue = errorMsg;
            this.markTestAsFailed();
        }
    },

    /**
     * @method
     * @description Compares two values for inequality based on both value and type. If values do match, the provided error message is bubbled up to the UI.
     * @param {any} actual
     * @param {any} expected
     * @param {string} errorMsg The message to display in the Unit Test UI.
     */
    assertNotEquals: function(actual, expected, errorMsg) {
        if (actual === expected) { 
            this.failValue = errorMsg;
            this.markTestAsFailed();
        }
    },

    /**
     * @method
     * @description Checks to see if the passed value is undefined or null.
     * @param {any} actual
     * @param {string} errorMsg The message to display in the Unit Test UI.
    */    
    assertUndefinedOrNull: function(actual, errorMsg) {
        if (actual !== undefined && actual !== null) {
            this.failValue = errorMsg;
            this.markTestAsFailed();
        }
    },

    /**
     * @method
     * @description Checks to see if the passed value is not undefined or null.
     * @param {any} actual
     * @param {string} errorMsg The message to display in the Unit Test UI.
    */    
    assertNotUndefinedOrNull: function(actual, errorMsg) {
        if (actual === undefined || actual === null) {
            this.failValue = errorMsg;
            this.markTestAsFailed();
        }
    },

    /**
     * @method
     * @description Checks to see if one string is contained within another.
     * @param {string} parentString The string in which we want to search for a value.
     * @param {string} subString The value for which we want to search.
     * @param {string} errorMsg The message to display in the Unit Test UI.
    */    
    assertContains: function(parentString, subString, errorMsg) {
        if (parentString.search(subString) < 0) {
            this.failValue = errorMsg;
            this.markTestAsFailed();
        }
    },

    /**
     * @method
     * @description Checks to see if one string is contained within another.
     * @param {string} parentString The string in which we want to search for a value.
     * @param {string} subString The value for which we want to search.
     * @param {string} errorMsg The message to display in the Unit Test UI.
    */    
    assertNotContains: function(parentString, subString, errorMsg) {
        if (parentString.search(subString) >= 0) {
            this.failValue = errorMsg;
            this.markTestAsFailed();
        }
    }

});


/**
 * @method
 * @member Ext.ux
 * @description To satisfy JSLint, we just pass the Ext.ux.UnitTest.Test() config to this method.
 * @param {object} config
 */
Ext.ux.NewUnitTest = function(config) {
    var foo = new Ext.ux.UnitTest.Test(config);
};

//// ==================================================================
//// A good unit-test framework should drink its own kool-aid.
//// Therefore, we will test the UnitTest.Test object assert methods.
//Ext.ux.NewUnitTest({
//    testName: 'Test assertEquals() method.',
//    testGroup: 'Ext.ux.UnitTest.Test',
//    testFunction: function(testObj) {
//        var stringVariable = '1';
//        var integerVariable = 1;
//        var floatingVariable = 1.00;
//
//        this.assertEquals(
//            stringVariable,
//            '1',
//            'stringVariable has unexpected value'
//        );
//
//        this.assertEquals(
//            integerVariable,
//            1,
//            'integerVariable has unexpected value'
//        );
//
//        this.assertEquals(
//            floatingVariable,
//            1.00,
//            'floatingVariable has unexpected value'
//        );
//    }
//});
//
//Ext.ux.NewUnitTest({
//    testName: 'Test assertNotEquals() method.',
//    testGroup: 'Ext.ux.UnitTest.Test',
//    testFunction: function(testObj) {
//        var stringVariable = '1';
//        var integerVariable = 1;
//        var floatingVariable = 1.00;
//
//        this.assertNotEquals(
//            stringVariable,
//            1,
//            'stringVariable has unexpected value'
//        );
//
//        this.assertNotEquals(
//            integerVariable,
//            '1',
//            'integerVariable has unexpected value'
//        );
//
//        this.assertEquals(
//            floatingVariable,
//            1,
//            'floatingVariable has unexpected value'
//        );
//    }
//});
//
//Ext.ux.NewUnitTest({
//    testName: 'Test assertUndefinedOrNull() method.',
//    testGroup: 'Ext.ux.UnitTest.Test',
//    testFunction: function(testObj) {
//        var nullVariable = null;
//
//        this.assertUndefinedOrNull(
//            nullVariable,
//            'nullVariable has unexpected value'
//        );
//    }
//});
//
//Ext.ux.NewUnitTest({
//    testName: 'Test assertNotUndefinedOrNull() method.',
//    testGroup: 'Ext.ux.UnitTest.Test',
//    testFunction: function(testObj) {
//        var stringVariable = '1';
//
//        this.assertNotUndefinedOrNull(
//            stringVariable,
//            'stringVariable has unexpected value'
//        );
//    }
//});
//
//Ext.ux.NewUnitTest({
//    testName: 'Test assertContains() method.',
//    testGroup: 'Ext.ux.UnitTest.Test',
//    testFunction: function(testObj) {
//        var stringOne = 'GFX International';
//        var stringTwo = 'GFX';
//
//        this.assertContains(
//            stringOne,
//            stringTwo,
//            'stringTwo was not found in stringOne'
//        );
//    }
//});
//
//Ext.ux.NewUnitTest({
//    testName: 'Test assertNotContains() method.',
//    testGroup: 'Ext.ux.UnitTest.Test',
//    testFunction: function(testObj) {
//        var stringOne = 'GFX International';
//        var stringTwo = 'GFX';
//
//        this.assertNotContains(
//            stringTwo,
//            stringOne,
//            'stringOne was found in stringTwo'
//        );
//    }
//});
//
//Ext.ux.NewUnitTest({
//    testName: 'Test assertTrue() method.',
//    testGroup: 'Ext.ux.UnitTest.Test',
//    testFunction: function(testObj) {
//        this.assertTrue(
//            true,
//            'assertTrue() is not working correctly.'
//        );
//    }
//});
//
//Ext.ux.NewUnitTest({
//    testName: 'Test assertFalse() method.',
//    testGroup: 'Ext.ux.UnitTest.Test',
//    testFunction: function(testObj) {
//
//        this.assertFalse(
//            false,
//            'assertFalse() is not working correctly.'
//        );
//    }
//});
//
//Ext.ux.NewUnitTest({
//    testName: 'Test assertIsGuid() method.',
//    testGroup: 'Ext.ux.UnitTest.Test',
//    testFunction: function(testObj) {
//
//        this.assertIsGuid(
//            '00000000-0000-0000-0000-000000000000',
//            'assertIsGuid() is not working correctly.'
//        );
//    }
//});
