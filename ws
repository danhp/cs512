COMP 512 : Programming Project Part 2

System Architecture & Special Features:

Part 1: The design of the first part of the project (web service) follows very closely the design that we were given. We left the code given in the rm package in that same package. We then created the middleware so it would have most of the same methods so the client would call the middleware web service the same way it would the resource managers. In other words, the logic for Car/Flight/Room-add/delete/query was put in the resource manager, the middleware only acting as a dispatching layer by selecting the right resource manager to send the request to.

ReserveItinerary is handled in the middleware. It simply contacts the individual resource managers to reserve the items. It will try to reserve has many items as possible without halting if one item is not available.

Finally, no extra concurrency handling step has been taken. The code we were given was already using 'synchronized' statements to block the Hashtable (which is also synchronized).

Part 2:

For extending the project for programming assignment 2, the middleware now stores an object of the TransactionManager, that will keep track of all ongoing transactions. As described, we added the start/commit/abort methods that will keep the TransactionManager up to date with all active transactions, and once the requests have been forwarded to the TM, we will then make a one phase commit/abort to the appropriate RM’s involved in the transaction. Whenever a request (operation) is being made, we will add it to the appropriate transaction, depicted by the ID argument of that request in the terminal prompt. In order to ensure safety, we have created an instance of the lockManager within the middleware, and whenever a modifying operation is being carried out within the middleware (ex: addFlight, deleteFlight), we will lock the data for each transaction. This is then unlocked within the commit/abort methods. 

Also, within the Middleware, we moved the customer logic from a separate server to the middleware instance itself. The customer hash map exists within the middleware, and the logic creating / removing customers also exists within the middleware.

The TransactionManger, stored within the middleware is given the proxies of the ResourceMangers, so it can forward the one phase commits/aborts to the appropriate RM. We have an enlist method, that when an operation (request) is called, it will add the appropriate relationship between the transactions and the RM’s involved with each transaction. 

Within the Resource Managers, we created methods start,commit, and abort. Commit and start will simply remove or add transactions into the RM local store - each RM has its own store of transactions that act with the local RM. When we write / remove data from the item hash table within the RM, we will add operations to the local transactions. Note, within the abort method, we will ‘undo’ each operation within the transaction: ex, if the method was an ADD method to the DB, we will then remove the data added, and if it was a REMOVE method, we will then add the data back. This is the reason for the local store within the RM’s, is so we can keep a history of the transactions, and abort them if we need to. Once they have been committed, the transaction will be removed, and abort will no longer be possible. 

Time-To-Live (TTL)


Maintain a table of transactions and their expire time.
Set the time when we start a new transaction.
Reset the timer every time a new operation comes in.
Every so often go through that list and and abort all the transaction whose expiretime is smaller than the currentTime.
