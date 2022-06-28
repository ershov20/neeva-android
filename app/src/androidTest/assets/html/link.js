colors = ["#ffcccc", "#ffffcc", "#ccffcc", "#ccffff", "#ccccff", "#ffccff"]

// Figure out what version of the page we're on.
params = new window.URLSearchParams(window.location.search)
page_index_str = params.get("page_index")
if (page_index_str == null) page_index_str = "1"
page_index = parseInt(page_index_str)

// Set up the title of the page so that tests can wait for the title to be set correctly.
document.title = "Page " + page_index

// Determine where the link should go.
next_page_index = page_index + 1
next_page_link = "?page_index=" + next_page_index
next_page_link_text = "Link to page " + next_page_index

function initializeLink() {
    document.body.style.background = colors[page_index % colors.length]

    // Sets up the page so that clicking on it navigates to a page with a higher index.
    document.querySelector("#next_page_link_element").href = next_page_link
    divElement = document.querySelector("#next_page_link_text_element")
    if (divElement) {
        divElement.innerText = next_page_link_text
    }
}