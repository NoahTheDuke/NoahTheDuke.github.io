// taken from https://capscollective.com/blog/bluesky-blog-comments/

function getPostLinkFromUri(uri) {
    return `https://bsky.app/profile/${uri.replace("at://", "").replace("/app.bsky.feed.post/", "/post/")}`;
}
function getImageLinkFromBlob(did, blobLink, useThumbnail) {
    return `https://cdn.bsky.app/img/${useThumbnail ? "feed_thumbnail" : "feed_fullsize"}/plain/${did}/${blobLink}`;
}
function createHtmlTextForRecord(record) {
    var htmlText = `<span>`

    const textEncoder = new TextEncoder();
    const utf8Decoder = new TextDecoder();
    const utf8Text = new Uint8Array(record.text.length * 3);
    textEncoder.encodeInto(record.text, utf8Text);

    var charIdx = 0;
    for (const facetIdx in record.facets) {
        const facet = record.facets[facetIdx];
        const facetFeature = facet.features[0];
        const facetType = facetFeature.$type;

        var facetLink = "#";
        if (facetType == "app.bsky.richtext.facet#tag") {
            facetLink = `https://bsky.app/hashtag/${facetFeature.tag}`;
        }
        else if (facetType == "app.bsky.richtext.facet#link") {
            facetLink = facetFeature.uri;
        }
        else if (facetType == "app.bsky.richtext.facet#mention") {
            facetLink = `https://bsky.app/profile/${facetFeature.did}`;
        }

        if (charIdx < facet.index.byteStart) {
            const preFacetText = utf8Text.slice(charIdx, facet.index.byteStart);
            htmlText += utf8Decoder.decode(preFacetText)
        }

        const facetText = utf8Text.slice(facet.index.byteStart, facet.index.byteEnd);
        htmlText += `<a class="comment-facet-link" href="${facetLink}" target="_blank">` + utf8Decoder.decode(facetText) + '</a>';

        charIdx = facet.index.byteEnd;
    }

    if (charIdx < utf8Text.length) {
        const postFacetText = utf8Text.slice(charIdx, utf8Text.length);
        htmlText += utf8Decoder.decode(postFacetText);
    }

    return htmlText + '</span>';
}
function createHtmlForReplies(replies) {
    var htmlText = ""
    for (const replyIdx in replies) {
        const reply = replies[replyIdx];
        const replyPost = reply.post;
        const record = replyPost.record;
        const postLink = getPostLinkFromUri(replyPost.uri);
        const postTimestamp = record.createdAt;
        const postText = createHtmlTextForRecord(record);

        var embedHtml = "";
        if (replyPost.embed) {
            if (replyPost.embed.$type == "app.bsky.embed.external#view") {
                const postEmbedExternal = replyPost.embed.external;
                if (postEmbedExternal.uri.includes(".gif?")) {
                    embedHtml += `<img class="comment-image" src="${postEmbedExternal.uri}" title="${postEmbedExternal.title}" alt="${postEmbedExternal.description}">`
                }
            }
            else if (replyPost.embed.$type == "app.bsky.embed.images#view") {
                const images = replyPost.record.embed.images;
                for (const imageIdx in images) {
                    const image = images[imageIdx];
                    const thumbnailLink = getImageLinkFromBlob(replyPost.author.did, image.image.ref.$link, true);
                    const fullSizelLink = getImageLinkFromBlob(replyPost.author.did, image.image.ref.$link, false);
                    embedHtml += `<a href="${fullSizelLink}" target="_blank"><img class="comment-image" src="${thumbnailLink}" alt="${image.alt}"></a>`
                }
            }
        }

        htmlText += `
            <div class="comment-block">
            <span class="comment-title"><img src="${replyPost.author.avatar}" class="comment-avatar"> ${replyPost.author.displayName} <a href="https://bsky.app/profile/${replyPost.author.did}" target="_blank">@${replyPost.author.handle}</a></span><span class="comment-text"> - ${(new Date(postTimestamp)).toLocaleString()}</span>
            <div class="comment-text">${postText}</div>
            ${embedHtml}
            <div class="comment-links">
                <span><a class="comment-link comment-reply-link" href="${postLink}" target="_blank">${replyPost.replyCount} repl${replyPost.replyCount !== 1 ? "ies" : "y"}</a></span>
            </div>
            <div class="comment-reply">
            ${createHtmlForReplies(reply.replies)}
            </div>
            </div>`
    }
    return htmlText;
}

function httpGetAsJsonAsync(url, callback) {
    var xmlHttp = new XMLHttpRequest();
    xmlHttp.onreadystatechange = function() { 
        if (xmlHttp.readyState == 4 && xmlHttp.status == 200) {
            var responseJson = JSON.parse(xmlHttp.responseText);
            if (typeof(responseJson) == 'undefined') {
                return;
            }
            var element = document.getElementById("show-comments");
            element.parentNode.removeChild(element);
            callback(responseJson);
        }
    }
    xmlHttp.open("GET", url, true);
    xmlHttp.send(null);
}
function responseCallback (responseJson) {
    const thread = responseJson["thread"];
    const threadPost = thread["post"];
    const postLink = getPostLinkFromUri(threadPost.uri);

    const threadReplies = thread.replies;
    document.getElementById("social-section").innerHTML = `
        <h4><b>Comments</b></h4>
        <p class="comment-text"><i>Reply on Bluesky <a href="${postLink}" target="_blank">here</a> to join the conversation.</i></p>
        <div class="comment-section">
        ${threadReplies.length > 0 ? createHtmlForReplies(threadReplies) : '<p class="comment-text">No comments to display</p>'}
        </div>`
}
