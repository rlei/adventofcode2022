(require '[clojure.string :as string])

; "0123" => [0 1 2 3]
(defn str-to-num-seq [s] (map #(- (int %1) (int \0)) s))

; "0100" or "0001" => "0101"
(defn bit-or-strings [s1 s2]
  (->> (map vector (str-to-num-seq s1) (str-to-num-seq s2))
       (map #(bit-or (first %1) (second %1)))
       string/join))

; ["123" "456" "789"] => ["147" "258" "369"]
(defn transpose [rows] (apply map vector rows))

; the core logic: [0 1 2 3 0 4] => "111101"
(defn visible-trees [s]
  (->>
   (reduce #(if (> %2 (first %1))
              [%2 (str (second %1) 1)]
              [(first %1) (str (second %1) 0)])
           [-1 ""]   ; [tallest tree, visible trees so far]
           s)
   second))

; "012304" => "111101"
(defn str-to-visible-trees [s] (->> s str-to-num-seq visible-trees))

(defn str-to-visible-trees-two-way [s]
  (bit-or-strings (str-to-visible-trees s) (reverse (str-to-visible-trees (reverse s)))))

(defn rows-to-visible-trees [rows] (map str-to-visible-trees-two-way rows))

(defn square-to-visible-trees [rows]
  (map bit-or-strings
       (rows-to-visible-trees rows)
       (transpose (rows-to-visible-trees (transpose rows)))))

(defn count-ones [xs] (->> (string/join xs) str-to-num-seq (reduce +)))

; the main
(println
 (->> (line-seq (java.io.BufferedReader. *in*))
      square-to-visible-trees
      count-ones))
