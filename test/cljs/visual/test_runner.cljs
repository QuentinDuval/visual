(ns visual.test-runner
  (:require
   [doo.runner :refer-macros [doo-tests]]
   [visual.core-test]
   [visual.common-test]))

(enable-console-print!)

(doo-tests 'visual.core-test
           'visual.common-test)
