"use client"
import {useSearchParams} from "next/navigation";
import {SearchBar} from "@/components/searchBar";
import {twMerge} from "tailwind-merge";

function TopBar() {
    return (
        <>
            {/* Top search bar and logo */}
            <div className={
                twMerge(
                    "flex flex-row",
                    "mx-12 my-4 space-x-2",
                )}>

                {/* Lumos Logo */}
                <div className="flex flex-row text-xl font-extrabold font-mono text-center items-center">
                    <div className={"tracking-widest"}>Lumos</div>
                    <div className="text-md">💡</div>
                </div>

                {/* Search Bar */}
                <SearchBar variant={"search"} />
            </div>
        </>
    )
}

export default function Page() {
    const searchParams = useSearchParams();
    return (<div>
        {/* Logo and search bar */}
        <TopBar />


    </div>);
}