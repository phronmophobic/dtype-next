(ns tech.v3.datatype.mmap
  (:require [tech.v3.datatype.native-buffer]
            [clojure.tools.logging :as log])
  (:import [tech.v3.datatype.native_buffer NativeBuffer]))


(def ^:private mmap-fn* (atom nil))


(defn set-mmap-impl!
  "Set the system mmap implementation to use.  There are three provided under the
  mmap directory -
  * `mmodel` - Use the JDK-16 memory model.  You have to have the module enabled.
  * `larray` - Use LArray mmap - default if available.

  If unset then the system will try to load the jdk-16 namespace, then larray,
  and finally nio."
  [mmap-fn]
  (reset! mmap-fn* mmap-fn))


(defn- resolve-mmap-fn
  []
  (swap! mmap-fn* (fn [existing-fn]
                    (if existing-fn
                      existing-fn
                      (let [jdk-16-fn (when (.startsWith (System/getProperty "java.version") "16-")
                                        (try
                                          (requiring-resolve 'tech.v3.datatype.mmap.mmodel/mmap-file)
                                          (catch Throwable e
                                            (log/warn "Failed to activate JDK-16 mmodel pathway; falling back to larray."))))]
                        (if jdk-16-fn
                          jdk-16-fn
                          (requiring-resolve 'tech.v3.datatype.mmap.larray/mmap-file)))))))


(defn mmap-file
  "Memory map a file returning a native buffer.  fpath must resolve to a valid
   java.io.File.
  Options
  * :resource-type - maps to tech.v3.resource `:track-type`, defaults to :auto.
  * :mmap-mode
    * :read-only - default - map the data as shared read-only.
    * :read-write - map the data as shared read-write.
    * :private - map a private copy of the data and do not share."
  (^NativeBuffer [fpath options]
   ((resolve-mmap-fn) fpath options))
  (^NativeBuffer [fpath]
   (mmap-file fpath {})))
