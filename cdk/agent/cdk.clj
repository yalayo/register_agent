(ns agent.cdk
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [stedi.cdk.alpha :as cdk]
            [uberdeps.api :as uberdeps]))

(cdk/import [[App Construct Duration Stack] :from "@aws-cdk/core"]
            [[Bucket] :from "@aws-cdk/aws-s3"]
            [[Code Function Runtime Tracing] :from "@aws-cdk/aws-lambda"]
            [[LambdaRestApi] :from "@aws-cdk/aws-apigateway"])

(defn- clean
  []
  (let [f (io/file "classes")]
    (when (.exists f)
      (->> f
           (file-seq)
           (reverse)
           (map io/delete-file)
           (dorun)))))

(def code
  (let [jarpath "target/app.jar"
        deps    (edn/read-string (slurp "deps.edn"))]
    (with-out-str
      (clean)
      (io/make-parents "classes/.")
      (io/make-parents jarpath)
      (compile 'agent.data)
      (compile 'agent.lambda)
      (uberdeps/package deps jarpath {:aliases [:classes]}))
    (Code/fromAsset jarpath)))

(def app (App))

(def stack (Stack app "register-agent-lambda"))

(def bucket (Bucket stack "register-agent-lambda-bucket"))

(def register-agent-fn
  (Function stack
            "register-agent-fn"
            {:code        code
             :handler     "com.busqandote.agent.RegisterAgent"
             :runtime     (:JAVA_8 Runtime)
             :environment {"BUCKET" (:bucketName bucket)}
             :memorySize 512
             :timeout (Duration/seconds 30)
             }))

(Bucket/grantWrite bucket register-agent-fn)

(def api (LambdaRestApi stack "register-agent-api" {:handler register-agent-fn}))

