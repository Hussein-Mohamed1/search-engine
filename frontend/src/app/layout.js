import {Nunito, Nunito_Sans, Open_Sans} from "next/font/google";
import "./globals.css"
const openSans = Open_Sans({
    subsets: ["latin"], variable: "--font-open-sans", display: "swap",
});

const nunito = Nunito({
    subsets: ["latin"], variable: "--font-nunito", display: "swap",
});

const nunitoSans = Nunito_Sans({
    subsets: ["latin"], variable: "--font-nunito-sans", display: "swap",
});

export const metadata = {
    title: "Lumos: Search Engine",
    description: "A search engine for the web made as a project for the course Advanced Programming Techniques (APT) at Cairo University.",
};

export default function RootLayout({children}) {
    return (<html lang="en">
        <body
            className={`${nunito.variable} ${openSans.variable} ${nunitoSans.variable} font-sans antialiased`}
        >
            {children}
        </body>
    </html>);
}