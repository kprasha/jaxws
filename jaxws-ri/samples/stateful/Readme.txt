This sample shows the JAX-WS RI's stateful webservice support feature.
Built on top of WS-Addressing, this mechanism allows the application
to easily maintain state on server-side objects, and allows clients to
distinguish instances.

See the javadoc of the server and the client for more details.

* To run
    * set JAXWS_HOME to the JAX-WS installation directory
    * ant server - runs apt to generate server side artifacts and
      does the deployment
    * ant client run - compiles the client application and runs it.

* Prerequisite

Refer to the Prerequisites defined in samples/docs/index.html.
