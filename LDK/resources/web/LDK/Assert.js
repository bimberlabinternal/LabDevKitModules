/**
 * This is designed to be used similar to asserts in java code.  Checks can be introduced
 * in your client-side code that will be performed when that code is executed.  If the assert fails,
 * a message will be logged to the server's error log.
 */
LDK.Assert = new function(){
    return {
        /**
         * Asserts the expected value equals the actual value, using equality (==)
         * @param {string} msg A message that can provide additional description of the error
         * @param {object} expected The expected value
         * @param {object} actual The actual value
         */
        assertEquality: function(msg, expected, actual){
            if (expected != actual){
                var message = 'Assert failed: ' + msg + '.  Expected: ' + expected + ', actual: ' + actual;
                console.error(message);
                LDK.Utils.logToServer({
                    message: message,
                    level: 'ERROR',
                    includeContext: true
                });
            }
        },

        /**
         * Asserts the expected value equals the actual value, using identity (===)
         * @param {string} msg A message that can provide additional description of the error
         * @param {object} expected The expected value
         * @param {object} actual The actual value
         */
        assertIdentity: function(msg, expected, actual){
            if (expected !== actual){
                var message = 'Assert failed: ' + msg + '.  Expected: ' + expected + ', actual: ' + actual;
                console.error(message);
                LDK.Utils.logToServer({
                    message: message,
                    level: 'ERROR',
                    includeContext: true
                });
            }
        },

        /**
         * Asserts the passed value is empty, as performed by Ext's isEmpty()
         * @param {string} msg A message that can provide additional description of the error
         * @param {object} value The value to be tested
         */
        assertNotEmpty: function(msg, value){
            if (LABKEY.ExtAdapter.isEmpty(value)){
                var message = 'Assert failed: ' + msg + '.  Value was empty: "' + value + '"';
                console.error(message);
                LDK.Utils.logToServer({
                    message: message,
                    level: 'ERROR',
                    includeContext: true
                });
            }
        },

        /**
         * Asserts the passed value is empty, as performed by Ext's isEmpty()
         * @param {string} msg A message that can provide additional description of the error
         * @param {object} value The value to be tested
         */
        assertEmpty: function(msg, value){
            if (!LABKEY.ExtAdapter.isEmpty(value)){
                var message = 'Assert failed: ' + msg + '.  Value was not empty: "' + value + '"';
                console.error(message);
                LDK.Utils.logToServer({
                    message: message,
                    level: 'ERROR',
                    includeContext: true
                });
            }
        },

        /**
         * Asserts the passed value is empty, as performed by Ext's isEmpty()
         * @param {string} msg A message that can provide additional description of the error
         * @param {object} condition The condition that should test true
         */
        assertTrue: function(msg, condition){
            if (!condition){
                var message = 'Assert true failed: ' + msg + '.';
                console.error(message);
                LDK.Utils.logToServer({
                    message: message,
                    level: 'ERROR',
                    includeContext: true
                });
            }
        }
    }
};