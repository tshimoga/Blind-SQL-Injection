Author
-------------------------------------------------
Name: Tarun Shimoga
ASU ID: tshimoga
ASU #: 1208478709


Initial Setup
------------------------------------------------

1. The tool needs a vulnerable application to be deployed and running. 
2. For that, you could either deploy the test case attached that was provided by the Professor or the login form present.
3. There are pre compiled classes in the bin directory of the blind_sql_injection folder.
4. Make sure you've entries in the database so that form queries have a chance to find those based on the values provided.

Execution
------------------------------------------------

For executing the tool, you need to provide the following
1. Vulnerable url
2. Form parameters in key1,value1;key2,value2 pairs
3. Success message
4. Failure message
5. Type of request (get/post)
For e.g.
If you deployed the vulnerable application provided, form the bin directory you'd execute 
java main.BlindSqlInjection "http://localhost:8080/vulnerable_php_application/login.php" "username,tshimoga;password,tarun" "Welcome" "Wrong password" "get" 