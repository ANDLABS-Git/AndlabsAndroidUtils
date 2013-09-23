AndroidUtils
============

Logging:
Log simple strings or formatted strings in one simple call:
        final String formattedTestString = "first argument = %s, second argument = %s";
        final String firstArgument = "abc";
        final Object secondArgument = new  Object() {
            public String toString() {
                return "123";
            };
        };
        
        L.d(formattedTestString, firstArgument, secondArgument);
        
        
Which results in a log output 
09-23 17:41:30.508: D/TestActivity:26(18510): onCreate(): first argument = abc, second argument = 123
