(ns cljs.user
  "Commonly used symbols for easy access in the ClojureScript REPL during
  development."
  (:require
    [cljs.repl :refer (Error->map apropos dir doc error->str ex-str ex-triage
                       find-doc print-doc pst source)]
    [clojure.pprint :refer (pprint)]
    [clojure.string :as str]))

(comment

  {1 {:id 1, :label "All", :source 1, :resonance 0, :notes "", :links #{}, :touched false}, 2 {:id 2, :label "Minddrop", :source 1, :resonance 0, :notes "Something else. And then...  wait. There is something here.  y", :links #{}, :touched true}, 6 {:id 6, :label "Focus Button", :source 4, :resonance 0, :notes "", :links #{}, :touched false}, 7 {:id 7, :label "Oops Drop", :source 6, :resonance 0, :notes "", :links #{}, :touched false}, 8 {:id 8, :label "", :source 6, :resonance 0, :notes "", :links #{}, :touched false}, 9 {:id 9, :label "Second Drop", :source 6, :resonance 0, :notes "", :links #{}, :touched false}, 10 {:id 10, :label "test", :source 1, :resonance 0, :notes "test notes", :links #{}, :touched true}, 11 {:id 11, :label "Where am I?", :source 1, :resonance 0, :notes "can it change? let's try again! asfasdf ", :links #{}, :touched true}, 12 {:id 12, :label "2", :source 1, :resonance 0, :notes "Something else. And then there's more~", :links #{}, :touched true}, 13 {:id 13, :label "Tasks", :source 1, :resonance 0, :notes "", :links #{}, :touched true}, 14 {:id 14, :label "Shower", :source 13, :resonance 0, :notes "", :links #{}, :touched false}, 15 {:id 15, :label "Make Breakfast", :source 13, :resonance 0, :notes "", :links #{}, :touched false}, 16 {:id 16, :label "Read Joy of Clojure", :source 13, :resonance 0, :notes "", :links #{}, :touched false}, 17 {:id 17, :label "Wash Face", :source 14, :resonance 0, :notes "", :links #{}, :touched false}, 18 {:id 18, :label "Wash Eyes", :source 14, :resonance 0, :notes "", :links #{}, :touched false}, 19 {:id 19, :label "Focus Mode", :source 2, :resonance 0, :notes "", :links #{}, :touched true}, 20 {:id 20, :label "Notes", :source 19, :resonance 0, :notes "", :links #{}, :touched false}, 21 {:id 21, :label "Explore Mode", :source 2, :resonance 0, :notes "", :links #{}, :touched false}, 22 {:id 22, :label "Inner/Outer Drop Indicators", :source 21, :resonance 0, :notes "", :links #{}, :touched false}, 23 {:id 23, :label "Guinea Pig", :source 1, :resonance 0, :notes "", :links #{}, :touched true}, 24 {:id 24, :label "New Guy", :source 1, :resonance 0, :notes "", :links #{}, :touched false}, 25 {:id 25, :label "and otherwise?", :source 1, :resonance 0, :notes "", :links #{}, :touched false}, 26 {:id 26, :label "deleteme", :source 24, :resonance 0, :notes "", :links #{}, :touched false}}


  :rcf)