# TodoMVC - Om.Next, Datomic, Datascript, SSE

This is a Clojure-based MVC application which covers usage of re-frame and Reagent with Datomic db.

## Instructions for running

### Backend
In order to run the datomic server, we need to have datomic pro (starter) installed. Then we need to navigate to 
datomic's root directory and run:

```bin/run -m datomic.peer-server -h localhost -p 8998 -a testtodos,testtodos -d todos,datomic:mem://todos```

This will start Datomic's peer server and we'll be able to run transactions.

Then we can start our server by navigating to out projects root directory and running:

```clj -Alocal```

The server should now be up and running.


### Frontend
We use shadow-cljs to build our frontend's JavaScript (from CLJS). To compile frontend, we first need to run
`npm install` to get all necessary JS packages. Then we can run `shadow-cljs watch local` to start watching the `local`
build. Any changes we make are hot-reloaded to the app. The main JS file is `resources/public/js/main.js`.
There are other files as well which support the TODO common ui operations. These should not be moved in order for the
app to work as expected.
Now the app should be up and running and we can perform some basic operatiuons with our TODO list.

## Implemented parts
I did not have time to implement all of the funtionalities. The one which work are:
adding new note, updating title of a note, toggling a note's status, deleting a note.
Other functions can be easily implemented if needed.

