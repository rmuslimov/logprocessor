(ns logprocessor.core-test
  (:require [logprocessor.core :refer :all]
            [midje.sweet :refer :all]))

(def test-et-xml "test/soap-request-11tECVXBzAQt4gQ2Oli4WE.xml")
(def test-ping-xml "test/soap-request-13Za5C1MoWtNhopyZHI7Xc.xml")
(def test-error-xml "test/soap-response-4YQ7mABXSVTnllMUtIHb6b.xml")
