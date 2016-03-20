(ns logprocessor.utils-test
  (:require [logprocessor.utils :as sut]
            [midje.sweet :refer :all]))

(facts "Check list s3 objects..."
  (sut/list-s3-objects :bcd1 :fokker 2016 2) => (repeat 29 {:source 1 :name 1})
  (provided
    (sut/list-s3-objects-for-date anything anything anything) =>
    [{:source 1 :name 1}])

  (sut/list-s3-objects :bcd1 :fokker 2016 2 1) => (list {:source 1 :name 1})
  (provided
    (sut/list-s3-objects-for-date anything anything anything) =>
    [{:source 1 :name 1}]))
