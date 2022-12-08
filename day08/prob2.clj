; "0123" => [0 1 2 3]
(defn str-to-num-seq [s] (map #(- (int %1) (int \0)) s))

; ["123" "456" "789"] => ["147" "258" "369"]
(defn transpose [rows] (apply map vector rows))

; viewing distance for xs[idx], looking from right to left
(defn viewing-distance [xs idx]
  (let [n (nth xs idx)]
    (loop [idx idx, distance 0]
      (cond
        (zero? idx) distance
        (>= (nth xs (dec idx)) n) (inc distance)
        :else (recur (dec idx) (inc distance))))))

; the core logic: [7 7 6 5 9] => [0 1 1 1 4], the viewing distances looking from *right* to *left*
(defn viewing-distances [xs]
  (map #(viewing-distance xs %1) (range (count xs))))

(defn viewing-scores-two-way-multiplied [xs]
  (map * (viewing-distances xs) (reverse (viewing-distances (reverse xs)))))

(defn rows-to-viewing-scores [rows] (map viewing-scores-two-way-multiplied rows))

(defn square-to-viewing-scores [rows]
  (map (partial map *)
       (rows-to-viewing-scores rows)
       (transpose (rows-to-viewing-scores (transpose rows)))))

; the main
(println
 (->> (line-seq (java.io.BufferedReader. *in*))
      (map str-to-num-seq)
      square-to-viewing-scores
      flatten
      (apply max)))
