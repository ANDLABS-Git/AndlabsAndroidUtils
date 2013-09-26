AndroidUtils
============
A set of handy Android utilities.  
Currently contains:  
-Helper for logging  
-Helper for fetching Interstitial ads


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
  
  


Interstitial ads:  
Make sure to prepare your AndroidManifest.xml like in the samples: https://github.com/ANDLABS-Git/AndroidUtils/blob/master/sample/AndroidManifest.xml

Call:

    AdUtils.getInstance(TestActivity.this).requestInterstitial();

Calling this the first time will initiate the caching of the ads. Calling it once ads are cached will show an ad. 
