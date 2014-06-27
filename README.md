domain-discovery module for Jenkins
=======================

This module helps organizations discover Jenkins instances running in their domain.
This is done by registering a domain name `discover-jenkins.acme.com` and have it point to a web application.
Jenkins instances running in `**.acme.com` (such as `foo.acme.com` or `foo.bar.acme.com`) will report its location
in the `Referer` header to a POST request to `http://discover-jenkins.acme.com`.
