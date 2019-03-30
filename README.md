## Development && Deployment
```
$ yarn
$ shadow-cljs watch app
;; wait for first compile to finish or expo gets confused
+$ yarn start+
$ expo start -c --lan --no-dev ;; does --lan or --no-dev actually do anything useful?


# in emacs editor
# cider-connect (localhost:8202 or w/e the build says the nrepl is running on)

# Connect the client device and let it load javascript bundle
# +(cemerick.piggieback/cljs-repl :app) - this is an override I guess+
# actually just use (shadow/nrepl-select :app)

# it should connect and be ready to evaluate

;; production build
# $ shadow-cljs release app # doesn't work right now
# expo build:android and expo-publish after cancelling the watch command
# this is publishing the dev code but who cares rn
```
