import {Nunito, Nunito_Sans} from "next/font/google";
import "./globals.css";

const nunito = Nunito({
    subsets: ["latin"],
});

const nunitoSans = Nunito_Sans({
    subsets: ["latin"],
});

export const metadata = {
    title: "Lumos: Search Engine",
    description: "A search engine for the web made as a project for the course Advanced Programming Techniques (APT) at Cairo University.",
};

export default function RootLayout({children}) {
    return (<html lang="en">
        <body
            className={`${nunito.className} ${nunitoSans.className} antialiased`}
        >
            {children}
        </body>
    </html>);
}
