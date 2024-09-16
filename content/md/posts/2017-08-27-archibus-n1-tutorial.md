{:title "Archibus and the SELECT N+1 Problem"
 :date "2017-08-27T12:51"
 :tags ["archibus" "javascript" "programming" "tutorial"]}

This past week, I had the opportunity to contribute to an existing custom report built for a client. It loaded incredibly slowly, so in between working on the contribution I had been tasked with, I also took it upon myself to speed it up. I think it's time to share what I've learned.

# Caveat

A big problem is that some portions of Archibus' Web Central are poorly documented, so understanding exactly how a given feature works in detail can be time-consuming and even with effort subtleties can be missed. Don't just take my word for how any given element works or doesn't work.

That being said, I'm here to help you make certain parts of your Archibus app run fast and beautiful, and blow away everyone who uses them.

# Overview

When creating reports for large data sets or building an exporter for a client-side piece of software, speed is a necessity. 5-10 second loads for an axvw or for an export is not okay and will hinder both development and usage. The major issue for a lot of pages are the classic [SELECT N+1 problem](https://stackoverflow.com/questions/97197/what-is-n1-select-query-issue) in disguise.

# SELECT N+1 Problem

The SELECT N+1 Problem refers to the issue where queries to the database are nested such that for each top-level query, a new query is made. As a very dumb example (that explicitly ignores JOINs for now):

```javascript
// Build a restriction from the two dates in the console
var restriction = new Ab.view.Restriction();
restriction.addClause('wr.date_requested', form.date_requested.from, '>=');
restriction.addClause('wr.date_requested', form.date_requested.to, '<=');

// Get all of the work request records within the restriction
var wrRecords = View.dataSources.get('wrDS').getRecords(restriction);

var blDS = View.dataSources.get('blDS');

// Loop over each record
for (var i = 0; i < wrRecords.length; i++) {
    var wrRec = wrRecords[i];

    // Do stuff with wrRec here

    // Build a restriction from the bl_id in each work request record
    var blId = wrRec.getValue('wr.bl_id');
    var blRes = new Ab.view.Restriction();
    blRes.addClause('bl.bl_id', blId);

    // Get all of the building records with that bl_id
    // NOTE: OBVIOUSLY, THIS IS DUMB AND CONTRIVED.
    // (Honestly, I can't remember right now if you need to do
    // blDS.clearRestrictions() :shrug:)
    var blRecords = blDS.getRecords(blRes);

    // Loop over each building record
    for (var j = 0; j < blRecords.length; j++) {
        var blRec = blRecords[j];

        // Do work here
    }
}
```

In this contrived example, we get all of the work requests requested in a given period, and then iterate over the work requests, extract the building id, and then grab all of the buildings with that building id and do work on them each. It demonstrates the SELECT N+1 Problem because we are making 1 request for the top level (work requests), and then making individual requests for each record we received (N number of requests).

For a slightly more robust example, in the DataView javascript (ab-core/controls/ab-data-view.js), `DataViewLevel.setRecords()` calls `this.nextLevel.refresh()`, which recursively calls `this.nextlevel.setRecords()`. If you've defined two (if not more god help you) levels, you're going to be calling setRecords with the results of your first query, and then for each restriction you've determined (either by each record containing the next record in `record.toRestriction()` or by monkey-patching `getNextLevelRestriction()` in your own js file). With two levels, that's N+1. If you define 3, such as by Work Request, by Building, by Craftsperson, you're up to (N \* N) + 1.

For a report of 5 items each with 5 sub records, that's not horrible but it's certainly not ideal. For a report with 100 items, each with up to 5 sub records? Unreasonable.

# Solution

To solve this problem, we want to rely on two things:

-   SQL
-   Javascript

With SQL, because of how it's designed internally (regardless of platform), it is pure speed when building sets of data. So in our above contrived example, instead of passing in a restriction for the `wr_id` and then quering for each `bl_id` individually, let's aggregate all building ids that are returned, and select them all at once:

```javascript
// Initially just as above
var restriction = new Ab.view.Restriction();
restriction.addClause('wr.date_requested', form.date_requested.from, '>=');
restriction.addClause('wr.date_requested', form.date_requested.to, '<=');

var wrRecords = View.dataSources.get('wrDS').getRecords(restriction);

// Here, we loop over the retrieved wr records and grab just the bl_id.
var blIds = wrRecords.map(function(rec) {
    return rec.getValue('wr.bl_id');
});

// Restriction.addClause can take in an array, and will "correctly" format the
// elements for the datasource field, wrapping the whole thing in `()`. If you
// don't trust Archibus to handle that correctly, a simple
//     var blIdRes = "('" + blIds.join("', '") + "')";
// will do in a pinch (tho this fucks up if the values you're concating have
// apostrophies in them, lol)

// Also note that this doesn't have to be the only restriction you pass in. If
// necessary, pass in any number of restrictions to this bottom-level dataSource,
// to further refine your search.
var blIdRes = new Ab.view.Restriction();
blIdRes.addClause('bl.bl_id', blIds, 'IN');

// Here we get all records from the bottom-level dataSource that fall under the
// combination of both restrictions. They come back in a potentially massive array
// of DataRecords, which is fine because the trip to the database is the actual
// source of the slow downs.
var tempRecords = View.dataSources.get('blDS').getRecords(blIdRes);

// One issue I've not mentioned yet is that these records will come back in *an*
// order, but without any information related to the totals of each subquery or any
// easy way to select within each having already fetched them. To solve this, we
// construct a plain javascript object (a dictionary) and use the different
// subquery elements (those individual items in the blIds array above) to act as
// the key to an array of the records from the bottom-level query.
var allBlRecords = {};
tempRecords.forEach(function(record, idx) {
    var key = record.getValue('wr.bl_id');
    if (key in allBlRecords) {
        allBlRecords[key].push(record);
    } else {
        allBlRecords[key] = [record];
    }
});

// Now we can enter our actual work for-loop, but this time with all information
// we need generated up front.
for (var i = 0; i < wrRecords.length; i++) {
    var wrRec = wrRecords[i];

    // Do stuff with wrRec here like before, but really cool stuff this time.

    // Notice here that all we're doing is retreiving an array of records from
    // a plain javascript object. This is an incredibly fast operation for any
    // browser (even IE 9 handles this faster than a database hit).
    var blRecords = allBlRecords[wrRec.getValue('wr.bl_id')];

    // Loop over each building record
    for (var j = 0; j < blRecords.length; j++) {
        var blRec = blRecords[j];

        // Do work here
    }
}
```

To make this work for something like the DataViewLevel, instead of monkey-patching the `getNextLevelRestriction()` as the docs suggest, monkey-patch `setRecords()` to perform the above directly, grabbing the necessary `level.getData` and `level.bodyXTemplate` up front, with calls to `renderRecord()` where needed, instead of letting Archibus' overly generic approach slow down your pages.

# Closing Remarks

When dealing with the database, always ask yourself, "How can I limit the total number of calls?" More than any other speed up you can write, that one will yield the most effective results for the lowest amount of effort. There are some very clever tricks that can be done in Java and javascript that will contribute to an overall snappier user experience, but if it takes 5+ seconds to load a single view or to generate a single exported file, your development time will slow down and your users will grow annoyed and uncomfortable.

Thanks for reading if you got this far. Let me know if you have any thoughts! I'm interested to see how others have tackled this problem.
