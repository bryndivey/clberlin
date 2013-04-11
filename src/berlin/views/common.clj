(ns berlin.views.common
  (:use [noir.core :only [defpartial]]
        [hiccup.page :only [include-css include-js html5]]))

(defpartial navbar []
  [:div.navbar.navbar-inverse.navbar-fixed-top
   [:div.navbar-inner
    [:div.container-fluid
     [:a.brand {:href "#"} "Berlin"]
     [:div.nav-collapse.collapse
      [:p.navbar-text.pull-right
       "Logged in as " [:a.navbar-link " Username"]]
      [:ul.nav
       [:li.active [:a "Home"]]
       [:li [:a "About"]]
       [:li [:a "Contact"]]]]]]])

(defpartial page-layout [& content]
            (html5
              [:head
               [:title "berlin"]
               (include-css "/css/reset.css")
               (include-css "/css/bootstrap.css")
               [:style {:type "text/css"}
                "
body {
        padding-top: 60px;
        padding-bottom: 40px;
      }
.sidebar-nav {
        padding: 9px 0;
}

 @media (max-width: 980px) {
        /* Enable use of floated navbar text */
        .navbar-text.pull-right {
          float: none;
          padding-left: 5px;
          padding-right: 5px;
        }
}"
                ]
               (include-css "/css/bootstrap-responsive.css")
               (include-css "/css/berlin.css")]
              [:body
               content
               (include-js
                "http://code.jquery.com/jquery.js"
                "/js/bootstrap.js"
                "/js/berlin.js")]))

(defpartial fluid-layout [sidebar & content]
  [:div.container-fluid
   [:div.row-fluid
    [:div.span3
     sidebar]
    [:div.span9
     content]]])

