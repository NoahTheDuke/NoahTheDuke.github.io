{:title "{{headline|safe}}"
 :date "{{publishedAt}}"
 :tags [{% for tag in tags|drop-last:1 %}{% safe %}"{{tag}}" {% endsafe %}{% endfor %}"{% safe %}{{tags|last}}{% endsafe %}"]
 :cohost-url "{{filename}}"}
{% if shares %}
{% for share in shares %}
**@{{share.author}}** posted:
{% if share.plain-body %}<div style="white-space: pre-line;">{{share.plain-body|safe}}</div>{% else %}{{share.body|safe}}{% endif %}
<hr>
{% endfor %}
{% endif %}
{% if body %}{{body|safe}}{% endif %}
