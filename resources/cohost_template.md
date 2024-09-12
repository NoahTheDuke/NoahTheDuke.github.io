{:title "{{headline|safe}}"
 :date "{{publishedAt}}"
 :tags [{% for tag in tags|drop-last:1 %}{% safe %}"{{tag}}" {% endsafe %}{% endfor %}"{% safe %}{{tags|last}}{% endsafe %}"]
 :cohost-id {{postId}}
 :cohost-url "{{filename}}"}
