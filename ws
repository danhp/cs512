WS

add/query/delete Logic in rms
reserve logic in middleware
customer in own rm -> how to delete customer
reserve itinerary
concurrency


The design of the first part of the project (web service) follows very closely the design that we were given. We left the code given in the rm package in that same package. We then created the middleware so it would have most of the same methods so the client would call the middleware web service the same way it would the resource managers. In other words, the logic for Car/Flight/Room-add/delete/query was put in the resource manager, the middleware only acting as a dispatching layer by selecting the right resource manager to send the request to.

The customers were handled by an additional server which actually contained the same code as the resource managers (obviously the middleware would only ask it about customers). A Hashtable stores the customers. The middleware doesn't contain too much logic about customers except for operations with side-effects. For example, newCustomer, newCustomerId and queryCustomerInfo forward the client request to the relevant customer server. However, deleteCustomer has to modify the reservable items if the targeted customer had any eservations. To handle this case, the middleware gets the list of customer reservations from the customer server. It then contacts the relevant resource managers to "unreserve" the item. Finally, the middleware requests the customer server to delete the customer.

ReserveItinerary is handled in the middleware. It simply contacts the individual resource managers to reserve the items. It will try to reserve has many items as possible without halting if one item is not available.

Finally, no extra concurrency handling step has been taken. The code we were given was already using 'synchronized' statements to block the Hashtable (which is also synchronized).
