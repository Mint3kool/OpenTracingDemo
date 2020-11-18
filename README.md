# OpenTracing Demo Repository

## Purpose
This repository was created to experiment with OpenTracing.

This contains examples of creating spans, child spans, adding tags, etc. Swagger was chosen as the framework for simple manual testing; it is not needed for the opentracing implementation.
It is simple enough to customize the API calls to point to whatever other external services are intended for use.

This repository is intended to be paired with its sister repository [here](https://github.com/Mint3kool/OpenTracingReceiver). The second repository was created to verify that even with unique global tracers context would be propogated properly.

## Prerequisites
* Jaeger Installation : https://www.jaegertracing.io/
* OpenJDK 1.8 or higher

This project was run on Fedora 32, using the Chrome version 86.0.4240.75.

## Terminal Run Instructions
These instructions are for running the projects on localhost

1. Clone/Fork this repository.
2. Clone/Fork the sister repository.
3. Navigate into the top level directory of each repository, in separate terminals.
4. Run the following command in both directories
```
 mvn package spring-boot:run
```
5. Start the Jaeger client
6. Navigate to localhost:8080 and click on the link in the page to get to the Swagger UI.
7. Click on `tracing-resource`
8. Execute whatever requests that you want to try
9. Navigate to the Jaeger UI, and refresh the page. The traces should appear.

## Additional Resources
OpenTracing API specs repo: https://github.com/opentracing/specification
Tutorials for Multiple languages: https://github.com/yurishkuro/opentracing-tutorial
A more in depth Java/Javascript OpenTracing walkthrough: https://github.com/opentracing-contrib/java-opentracing-walkthrough
